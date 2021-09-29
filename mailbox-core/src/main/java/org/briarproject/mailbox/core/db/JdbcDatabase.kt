package org.briarproject.mailbox.core.db

import org.briarproject.mailbox.core.api.Contact
import org.briarproject.mailbox.core.db.DatabaseConstants.Companion.DB_SETTINGS_NAMESPACE
import org.briarproject.mailbox.core.db.DatabaseConstants.Companion.DIRTY_KEY
import org.briarproject.mailbox.core.db.DatabaseConstants.Companion.LAST_COMPACTED_KEY
import org.briarproject.mailbox.core.db.DatabaseConstants.Companion.MAX_COMPACTION_INTERVAL_MS
import org.briarproject.mailbox.core.db.DatabaseConstants.Companion.SCHEMA_VERSION_KEY
import org.briarproject.mailbox.core.db.JdbcUtils.tryToClose
import org.briarproject.mailbox.core.settings.Settings
import org.briarproject.mailbox.core.system.Clock
import org.briarproject.mailbox.core.util.LogUtils.logDuration
import org.briarproject.mailbox.core.util.LogUtils.logException
import org.briarproject.mailbox.core.util.LogUtils.now
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.Arrays
import java.util.LinkedList
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.concurrent.GuardedBy

abstract class JdbcDatabase(private val dbTypes: DatabaseTypes, private val clock: Clock) :
    Database {

    internal companion object {

        private val LOG = LoggerFactory.getLogger(JdbcDatabase::class.java)
        const val CODE_SCHEMA_VERSION = 1

        private val CREATE_SETTINGS = """
                    CREATE TABLE settings
                           (namespace _STRING NOT NULL,
                           settingKey _STRING NOT NULL,
                           value _STRING NOT NULL,
                           PRIMARY KEY (namespace, settingKey))
        """.trimIndent()

        private val CREATE_CONTACTS = """
                    CREATE TABLE contacts
                           (contactId INT NOT NULL,
                           token _STRING NOT NULL,
                           inbox _STRING NOT NULL,
                           outbox _STRING NOT NULL)
        """.trimIndent()
    }

    private val connectionsLock: Lock = ReentrantLock()
    private val connectionsChanged = connectionsLock.newCondition()

    @GuardedBy("connectionsLock")
    private val connections = LinkedList<Connection>()

    @GuardedBy("connectionsLock")
    private var openConnections = 0

    @GuardedBy("connectionsLock")
    private var closed = false

    @Volatile
    private var wasDirtyOnInitialisation = false

    fun open(driverClass: String, reopen: Boolean, listener: MigrationListener?) {
        // Load the JDBC driver
        try {
            Class.forName(driverClass)
        } catch (e: ClassNotFoundException) {
            throw DbException(e)
        }
        // Open the database and create the tables and indexes if necessary
        val compact: Boolean
        var txn = startTransaction()
        try {
            compact = if (reopen) {
                val s: Settings = getSettings(txn, DB_SETTINGS_NAMESPACE)
                wasDirtyOnInitialisation = isDirty(s)
                migrateSchema(txn, s, listener) || isCompactionDue(s)
            } else {
                wasDirtyOnInitialisation = false
                createTables(txn)
                initialiseSettings(txn)
                false
            }
            if (LOG.isInfoEnabled) {
                LOG.info("db dirty? $wasDirtyOnInitialisation")
            }
            createIndexes(txn)
            commitTransaction(txn)
        } catch (e: DbException) {
            abortTransaction(txn)
            throw e
        }
        // Compact the database if necessary
        if (compact) {
            listener?.onDatabaseCompaction()
            val start: Long = now()
            compactAndClose()
            logDuration(LOG, { "Compacting database" }, start)
            // Allow the next transaction to reopen the DB
            synchronized(connectionsLock) { closed = false }
            txn = startTransaction()
            try {
                storeLastCompacted(txn)
                commitTransaction(txn)
            } catch (e: DbException) {
                abortTransaction(txn)
                throw e
            }
        }
    }

    /**
     * Compares the schema version stored in the database with the schema
     * version used by the current code and applies any suitable migrations to
     * the data if necessary.
     *
     * @return true if any migrations were applied, false if the schema was
     * already current
     * @throws DataTooNewException if the data uses a newer schema than the
     * current code
     * @throws DataTooOldException if the data uses an older schema than the
     * current code and cannot be migrated
     */
    @Throws(DbException::class)
    private fun migrateSchema(txn: Connection, s: Settings, listener: MigrationListener?): Boolean {
        var dataSchemaVersion = s.getInt(SCHEMA_VERSION_KEY, -1)
        if (dataSchemaVersion == -1) throw DbException()
        if (dataSchemaVersion == CODE_SCHEMA_VERSION) return false
        if (CODE_SCHEMA_VERSION < dataSchemaVersion) throw DataTooNewException()
        // Apply any suitable migrations in order
        for (m in getMigrations()) {
            val start: Int = m.startVersion
            val end: Int = m.endVersion
            if (start == dataSchemaVersion) {
                if (LOG.isInfoEnabled) LOG.info("Migrating from schema $start to $end")
                listener?.onDatabaseMigration()
                // Apply the migration
                m.migrate(txn)
                // Store the new schema version
                storeSchemaVersion(txn, end)
                dataSchemaVersion = end
            }
        }
        if (dataSchemaVersion != CODE_SCHEMA_VERSION) throw DataTooOldException()
        return true
    }

    // Public access for testing
    fun getMigrations(): List<Migration<Connection>> {
        return Arrays.asList<Migration<Connection>>(
            // Migration1_2(dbTypes),
        )
    }

    @Throws(DbException::class, SQLException::class)
    protected abstract fun createConnection(): Connection

    @Throws(DbException::class)
    protected abstract fun compactAndClose()

    override fun startTransaction(): Connection {
        var txn: Connection?
        connectionsLock.lock()
        txn = try {
            if (closed) throw DbClosedException()
            connections.poll()
        } finally {
            connectionsLock.unlock()
        }
        try {
            if (txn == null) {
                // Open a new connection
                txn = createConnection()
                txn.autoCommit = false
                connectionsLock.lock()
                try {
                    openConnections++
                } finally {
                    connectionsLock.unlock()
                }
            }
        } catch (e: SQLException) {
            throw DbException(e)
        }
        return txn
    }

    override fun abortTransaction(txn: Connection) {
        try {
            txn.rollback()
            connectionsLock.lock()
            try {
                connections.add(txn)
                connectionsChanged.signalAll()
            } finally {
                connectionsLock.unlock()
            }
        } catch (e: SQLException) {
            // Try to close the connection
            logException(LOG, e)
            tryToClose(txn, LOG)
            // Whatever happens, allow the database to close
            connectionsLock.lock()
            try {
                openConnections--
                connectionsChanged.signalAll()
            } finally {
                connectionsLock.unlock()
            }
        }
    }

    override fun commitTransaction(txn: Connection) {
        try {
            txn.commit()
        } catch (e: SQLException) {
            throw DbException(e)
        }
        connectionsLock.lock()
        try {
            connections.add(txn)
            connectionsChanged.signalAll()
        } finally {
            connectionsLock.unlock()
        }
    }

    @Throws(SQLException::class)
    fun closeAllConnections() {
        var interrupted = false
        connectionsLock.lock()
        try {
            closed = true
            for (c in connections) c.close()
            openConnections -= connections.size
            connections.clear()
            while (openConnections > 0) {
                try {
                    connectionsChanged.await()
                } catch (e: InterruptedException) {
                    LOG.warn("Interrupted while closing connections")
                    interrupted = true
                }
                for (c in connections) c.close()
                openConnections -= connections.size
                connections.clear()
            }
        } finally {
            connectionsLock.unlock()
        }
        if (interrupted) Thread.currentThread().interrupt()
    }

    private fun isDirty(s: Settings): Boolean {
        return s.getBoolean(DIRTY_KEY, false)
    }

    @Throws(DbException::class)
    fun setDirty(txn: Connection, dirty: Boolean) {
        val s = Settings()
        s.putBoolean(DIRTY_KEY, dirty)
        mergeSettings(txn, s, DB_SETTINGS_NAMESPACE)
    }

    private fun isCompactionDue(s: Settings): Boolean {
        val lastCompacted = s.getLong(LAST_COMPACTED_KEY, 0)
        val elapsed = clock.currentTimeMillis() - lastCompacted
        if (LOG.isInfoEnabled) LOG.info("$elapsed ms since last compaction")
        return elapsed > MAX_COMPACTION_INTERVAL_MS
    }

    @Throws(DbException::class)
    private fun storeSchemaVersion(txn: Connection, version: Int) {
        val s = Settings()
        s.putInt(SCHEMA_VERSION_KEY, version)
        mergeSettings(txn, s, DB_SETTINGS_NAMESPACE)
    }

    @Throws(DbException::class)
    private fun storeLastCompacted(txn: Connection) {
        val s = Settings()
        s.putLong(LAST_COMPACTED_KEY, clock.currentTimeMillis())
        mergeSettings(txn, s, DB_SETTINGS_NAMESPACE)
    }

    @Throws(DbException::class)
    private fun initialiseSettings(txn: Connection) {
        val s = Settings()
        s.putInt(SCHEMA_VERSION_KEY, CODE_SCHEMA_VERSION)
        s.putLong(LAST_COMPACTED_KEY, clock.currentTimeMillis())
        mergeSettings(txn, s, DB_SETTINGS_NAMESPACE)
    }

    @Throws(DbException::class)
    private fun createTables(txn: Connection) {
        var s: Statement? = null
        try {
            s = txn.createStatement()
            s.executeUpdate(dbTypes.replaceTypes(CREATE_SETTINGS))
            s.executeUpdate(dbTypes.replaceTypes(CREATE_CONTACTS))
            s.close()
        } catch (e: SQLException) {
            tryToClose(s, LOG)
            throw DbException(e)
        }
    }

    @Throws(DbException::class)
    private fun createIndexes(txn: Connection) {
        var s: Statement? = null
        try {
            s = txn.createStatement()
            // s.executeUpdate(INDEX_SOMETABLE_BY_SOMECOLUMN)
            s.close()
        } catch (e: SQLException) {
            tryToClose(s, LOG)
            throw DbException(e)
        }
    }

    @Throws(DbException::class)
    override fun getSettings(txn: Connection, namespace: String?): Settings {
        var ps: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            val sql = """
                SELECT settingKey, value FROM settings
                       WHERE namespace = ?
            """.trimIndent()

            ps = txn.prepareStatement(sql)
            ps.setString(1, namespace)
            rs = ps.executeQuery()
            val s = Settings()
            while (rs.next()) s[rs.getString(1)] = rs.getString(2)
            rs.close()
            ps.close()
            s
        } catch (e: SQLException) {
            tryToClose(rs, LOG)
            tryToClose(ps, LOG)
            throw DbException(e)
        }
    }

    @Throws(DbException::class)
    override fun mergeSettings(txn: Connection, s: Settings, namespace: String?) {
        var ps: PreparedStatement? = null
        try {
            // Update any settings that already exist
            var sql = """
                UPDATE settings SET value = ?
                       WHERE namespace = ? AND settingKey = ?
            """.trimIndent()

            ps = txn.prepareStatement(sql)
            for ((key, value) in s) {
                ps.setString(1, value)
                ps.setString(2, namespace)
                ps.setString(3, key)
                ps.addBatch()
            }
            var batchAffected = ps.executeBatch()
            if (batchAffected.size != s.size) throw DbStateException()
            for (rows in batchAffected) {
                if (rows < 0) throw DbStateException()
                if (rows > 1) throw DbStateException()
            }
            // Insert any settings that don't already exist
            sql = """
                INSERT INTO settings (namespace, settingKey, value)
                            VALUES (?, ?, ?)
            """.trimIndent()

            ps = txn.prepareStatement(sql)
            var updateIndex = 0
            var inserted = 0
            for ((key, value) in s) {
                if (batchAffected[updateIndex] == 0) {
                    ps.setString(1, namespace)
                    ps.setString(2, key)
                    ps.setString(3, value)
                    ps.addBatch()
                    inserted++
                }
                updateIndex++
            }
            batchAffected = ps.executeBatch()
            if (batchAffected.size != inserted) throw DbStateException()
            for (rows in batchAffected) if (rows != 1) throw DbStateException()
            ps.close()
        } catch (e: SQLException) {
            tryToClose(ps, LOG)
            throw DbException(e)
        }
    }

    @Throws(DbException::class)
    override fun addContact(txn: Connection, contact: Contact) {
        var ps: PreparedStatement? = null
        try {
            val sql = """INSERT INTO contacts (contactId, token, inbox, outbox)
                                VALUES (?, ?, ?, ?)
            """.trimIndent()
            ps = txn.prepareStatement(sql)
            ps.setInt(1, contact.id)
            ps.setString(2, contact.token)
            ps.setString(3, contact.inboxId)
            ps.setString(4, contact.outboxId)
            val affected = ps.executeUpdate()
            if (affected != 1) throw DbStateException()
            ps.close()
        } catch (e: SQLException) {
            tryToClose(ps, LOG)
            throw DbException(e)
        }
    }

    @Throws(DbException::class)
    override fun getContact(txn: Connection, id: Int): Contact? {
        var ps: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            val sql = """SELECT token, inbox, outbox FROM contacts
                                WHERE contactId = ?
            """.trimIndent()
            ps = txn.prepareStatement(sql)
            ps.setInt(1, id)
            rs = ps.executeQuery()
            if (!rs.next()) return null
            val token = rs.getString(1)
            val inboxId = rs.getString(2)
            val outboxId = rs.getString(3)
            rs.close()
            ps.close()
            return Contact(id, token, inboxId, outboxId)
        } catch (e: SQLException) {
            tryToClose(rs, LOG)
            tryToClose(ps, LOG)
            throw DbException(e)
        }
    }

    @Throws(DbException::class)
    override fun removeContact(txn: Connection, id: Int) {
        var ps: PreparedStatement? = null
        try {
            val sql = "DELETE FROM contacts WHERE contactId = ?"
            ps = txn.prepareStatement(sql)
            ps.setInt(1, id)
            val affected = ps.executeUpdate()
            if (affected != 1) throw DbStateException()
            ps.close()
        } catch (e: SQLException) {
            tryToClose(ps, LOG)
            throw DbException(e)
        }
    }

}
