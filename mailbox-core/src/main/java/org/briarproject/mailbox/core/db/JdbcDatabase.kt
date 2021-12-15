/*
 *     Briar Mailbox
 *     Copyright (C) 2021-2022  The Briar Project
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.briarproject.mailbox.core.db

import org.briarproject.mailbox.core.contacts.Contact
import org.briarproject.mailbox.core.db.DatabaseConstants.Companion.DB_SETTINGS_NAMESPACE
import org.briarproject.mailbox.core.db.DatabaseConstants.Companion.DIRTY_KEY
import org.briarproject.mailbox.core.db.DatabaseConstants.Companion.LAST_COMPACTED_KEY
import org.briarproject.mailbox.core.db.DatabaseConstants.Companion.MAX_COMPACTION_INTERVAL_MS
import org.briarproject.mailbox.core.db.DatabaseConstants.Companion.SCHEMA_VERSION_KEY
import org.briarproject.mailbox.core.db.JdbcUtils.tryToClose
import org.briarproject.mailbox.core.settings.Settings
import org.briarproject.mailbox.core.system.Clock
import org.briarproject.mailbox.core.util.LogUtils.info
import org.briarproject.mailbox.core.util.LogUtils.logDuration
import org.briarproject.mailbox.core.util.LogUtils.logException
import org.briarproject.mailbox.core.util.LogUtils.now
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.LinkedList
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.concurrent.GuardedBy
import kotlin.concurrent.withLock

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
                           outbox _STRING NOT NULL,
                           PRIMARY KEY (contactId))
        """.trimIndent()
    }

    /**
     * This lock is used for making sure that either one writable or alternatively an arbitrary
     * number of read-only transactions is in progress at the same time. It acts as a barrier
     * that blocks threads that try to start a transaction that cannot currently be started
     * because of that invariant.
     */
    private val transactionLock = ReentrantReadWriteLock(true)

    /**
     * In the case of readers, multiple threads can be accessing the database simultaneously.
     * To synchronise access to shared fields, we use this lock.
     *
     * Do not start any transactions while holding [connectionsLock] as this is deadlock-prone.
     */
    protected val connectionsLock: Lock = ReentrantLock()
    private val connectionsChanged = connectionsLock.newCondition()

    @GuardedBy("connectionsLock")
    private val connections = LinkedList<Connection>()

    @GuardedBy("connectionsLock")
    private var openConnections = 0

    @GuardedBy("connectionsLock")
    protected var closed = true

    @Volatile
    private var wasDirtyOnInitialisation = false

    /**
     * Returns true if the database already existed. It is not safe to call this method
     * concurrently with [dropAllTablesAndClose]
     */
    internal fun open(driverClass: String, listener: MigrationListener?): Boolean {
        try {
            Class.forName(driverClass)
        } catch (e: ClassNotFoundException) {
            throw DbException(e)
        }
        // Open the database and create the tables and indexes if necessary
        // First: mark the database as not closed
        connectionsLock.withLock {
            closed = false
        }
        // Check if we have the settings table to determine if we're reopening an existing database
        LOG.info { "checking for settings table" }
        val reopen = databaseHasSettingsTable()
        LOG.info {
            if (reopen) "settings table found, reopening"
            else "settings table not found, not reopening"
        }
        var compact = false
        write { txn ->
            val connection = txn.unbox()
            compact = if (reopen) {
                val s: Settings = getSettings(connection, DB_SETTINGS_NAMESPACE)
                wasDirtyOnInitialisation = isDirty(s)
                migrateSchema(connection, s, listener) || isCompactionDue(s)
            } else {
                wasDirtyOnInitialisation = false
                createTables(connection)
                initialiseSettings(connection)
                false
            }
            LOG.info { "db dirty? $wasDirtyOnInitialisation" }
            createIndexes(connection)
        }
        // Compact the database if necessary
        if (compact) {
            listener?.onDatabaseCompaction()
            val start: Long = now()
            compactAndClose()
            logDuration(LOG, start) { "Compacting database" }
            // Allow the next transaction to reopen the DB
            connectionsLock.withLock {
                closed = false
            }
            write { txn ->
                storeLastCompacted(txn.unbox())
            }
        }
        return reopen
    }

    @Throws(DbException::class, SQLException::class)
    protected abstract fun databaseHasSettingsTable(): Boolean

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
    private fun migrateSchema(
        connection: Connection,
        s: Settings,
        listener: MigrationListener?,
    ): Boolean {
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
                m.migrate(connection)
                // Store the new schema version
                storeSchemaVersion(connection, end)
                dataSchemaVersion = end
            }
        }
        if (dataSchemaVersion != CODE_SCHEMA_VERSION) throw DataTooOldException()
        return true
    }

    @Suppress("MemberVisibilityCanBePrivate") // visible for testing
    internal fun getMigrations(): List<Migration<Connection>> {
        return listOf(
            // Migration1_2(dbTypes),
        )
    }

    @Throws(DbException::class, SQLException::class)
    protected abstract fun createConnection(): Connection

    @Throws(DbException::class)
    protected abstract fun compactAndClose()

    /**
     * Starts a new transaction and returns an object representing it.
     *
     * This method acquires locks, so it must not be called while holding a
     * lock.
     *
     * @param readOnly true if the transaction will only be used for reading.
     */
    private fun startTransaction(readOnly: Boolean): Transaction {
        // Don't allow reentrant locking
        check(transactionLock.readHoldCount <= 0)
        check(transactionLock.writeHoldCount <= 0)
        val start = now()
        if (readOnly) {
            transactionLock.readLock().lock()
            logDuration(LOG, start) { "Waiting for read lock" }
        } else {
            transactionLock.writeLock().lock()
            logDuration(LOG, start) { "Waiting for write lock" }
        }
        return try {
            Transaction(startTransaction(), readOnly)
        } catch (e: DbException) {
            if (readOnly) transactionLock.readLock().unlock() else transactionLock.writeLock()
                .unlock()
            throw e
        } catch (e: RuntimeException) {
            if (readOnly) transactionLock.readLock().unlock() else transactionLock.writeLock()
                .unlock()
            throw e
        }
    }

    private fun startTransaction(): Connection {
        var connection = connectionsLock.withLock {
            if (closed) throw DbClosedException()
            connections.poll()
        }
        try {
            if (connection == null) {
                // Open a new connection
                connection = createConnection()
                connection.autoCommit = false
                connectionsLock.withLock {
                    openConnections++
                }
            }
        } catch (e: SQLException) {
            throw DbException(e)
        }
        return connection
    }

    private fun abortTransaction(connection: Connection) {
        try {
            connection.rollback()
            connectionsLock.withLock {
                connections.add(connection)
                connectionsChanged.signalAll()
            }
        } catch (e: SQLException) {
            // Try to close the connection
            logException(LOG, e) { "Error while aborting transaction" }
            tryToClose(connection, LOG)
            // Whatever happens, allow the database to close
            connectionsLock.withLock {
                openConnections--
                connectionsChanged.signalAll()
            }
        }
    }

    private fun commitTransaction(connection: Connection) {
        try {
            connection.commit()
        } catch (e: SQLException) {
            throw DbException(e)
        }
        connectionsLock.withLock {
            connections.add(connection)
            connectionsChanged.signalAll()
        }
    }

    @Throws(SQLException::class)
    fun closeAllConnections() {
        var interrupted = false
        connectionsLock.withLock {
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
        }
        if (interrupted) Thread.currentThread().interrupt()
    }

    private fun isDirty(s: Settings): Boolean {
        return s.getBoolean(DIRTY_KEY, false)
    }

    @Throws(DbException::class)
    fun setDirty(connection: Connection, dirty: Boolean) {
        val s = Settings()
        s.putBoolean(DIRTY_KEY, dirty)
        mergeSettings(connection, s, DB_SETTINGS_NAMESPACE)
    }

    private fun isCompactionDue(s: Settings): Boolean {
        val lastCompacted = s.getLong(LAST_COMPACTED_KEY, 0)
        val elapsed = clock.currentTimeMillis() - lastCompacted
        if (LOG.isInfoEnabled) LOG.info("$elapsed ms since last compaction")
        return elapsed > MAX_COMPACTION_INTERVAL_MS
    }

    @Throws(DbException::class)
    private fun storeSchemaVersion(connection: Connection, version: Int) {
        val s = Settings()
        s.putInt(SCHEMA_VERSION_KEY, version)
        mergeSettings(connection, s, DB_SETTINGS_NAMESPACE)
    }

    @Throws(DbException::class)
    private fun storeLastCompacted(connection: Connection) {
        val s = Settings()
        s.putLong(LAST_COMPACTED_KEY, clock.currentTimeMillis())
        mergeSettings(connection, s, DB_SETTINGS_NAMESPACE)
    }

    @Throws(DbException::class)
    private fun initialiseSettings(connection: Connection) {
        val s = Settings()
        s.putInt(SCHEMA_VERSION_KEY, CODE_SCHEMA_VERSION)
        s.putLong(LAST_COMPACTED_KEY, clock.currentTimeMillis())
        mergeSettings(connection, s, DB_SETTINGS_NAMESPACE)
    }

    @Throws(DbException::class)
    private fun createTables(connection: Connection) {
        var s: Statement? = null
        try {
            s = connection.createStatement()
            s.executeUpdate(dbTypes.replaceTypes(CREATE_SETTINGS))
            s.executeUpdate(dbTypes.replaceTypes(CREATE_CONTACTS))
            s.close()
        } catch (e: SQLException) {
            tryToClose(s, LOG)
            throw DbException(e)
        }
    }

    @Throws(DbException::class)
    private fun createIndexes(connection: Connection) {
        var s: Statement? = null
        try {
            s = connection.createStatement()
            // s.executeUpdate(INDEX_SOMETABLE_BY_SOMECOLUMN)
            s.close()
        } catch (e: SQLException) {
            tryToClose(s, LOG)
            throw DbException(e)
        }
    }

    @Throws(DbException::class)
    override fun dropAllTablesAndClose() {
        connectionsLock.withLock {
            if (closed) throw DbClosedException()
            var c: Connection? = null
            try {
                c = createConnection()
                // Prevent new transactions from starting, wait for any ongoing transactions to finish
                closeAllConnections()
                execute(c, "DROP TABLE settings")
                execute(c, "DROP TABLE contacts")
                c.close()
            } catch (e: SQLException) {
                tryToClose(c, LOG)
                throw DbException(e)
            }
        }
    }

    private fun execute(connection: Connection, sql: String) {
        var ps: PreparedStatement? = null
        try {
            ps = connection.prepareStatement(sql)
            ps.executeUpdate()
            ps.close()
        } catch (e: SQLException) {
            tryToClose(ps, LOG)
            throw DbException(e)
        }
    }

    @Throws(DbException::class)
    override fun getSettings(txn: Transaction, namespace: String): Settings {
        val connection: Connection = txn.unbox()
        return getSettings(connection, namespace)
    }

    @Throws(DbException::class)
    private fun getSettings(connection: Connection, namespace: String): Settings {
        var ps: PreparedStatement? = null
        var rs: ResultSet? = null
        return try {
            val sql = """
                SELECT settingKey, value FROM settings
                       WHERE namespace = ?
            """.trimIndent()

            ps = connection.prepareStatement(sql)
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
    override fun mergeSettings(txn: Transaction, s: Settings, namespace: String) {
        val connection: Connection = txn.unbox()
        mergeSettings(connection, s, namespace)
    }

    @Throws(DbException::class)
    fun mergeSettings(connection: Connection, s: Settings, namespace: String) {
        var ps: PreparedStatement? = null
        try {
            // Update any settings that already exist
            var sql = """
                UPDATE settings SET value = ?
                       WHERE namespace = ? AND settingKey = ?
            """.trimIndent()

            ps = connection.prepareStatement(sql)
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

            ps = connection.prepareStatement(sql)
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
    override fun addContact(txn: Transaction, contact: Contact) {
        val connection: Connection = txn.unbox()
        var ps: PreparedStatement? = null
        try {
            val sql = """INSERT INTO contacts (contactId, token, inbox, outbox)
                                VALUES (?, ?, ?, ?)
            """.trimIndent()
            ps = connection.prepareStatement(sql)
            ps.setInt(1, contact.contactId)
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
    override fun getContact(txn: Transaction, id: Int): Contact? {
        val connection: Connection = txn.unbox()
        var ps: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            val sql = """SELECT token, inbox, outbox FROM contacts
                                WHERE contactId = ?
            """.trimIndent()
            ps = connection.prepareStatement(sql)
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
    override fun getContacts(txn: Transaction): List<Contact> {
        val contacts = ArrayList<Contact>()
        val connection: Connection = txn.unbox()
        var ps: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            val sql = "SELECT contactId, token, inbox, outbox FROM contacts"
            ps = connection.prepareStatement(sql)
            rs = ps.executeQuery()
            while (rs.next()) {
                val id = rs.getInt(1)
                val token = rs.getString(2)
                val inboxId = rs.getString(3)
                val outboxId = rs.getString(4)
                contacts.add(Contact(id, token, inboxId, outboxId))
            }
            rs.close()
            ps.close()
            return contacts
        } catch (e: SQLException) {
            tryToClose(rs, LOG)
            tryToClose(ps, LOG)
            throw DbException(e)
        }
    }

    @Throws(DbException::class)
    override fun removeContact(txn: Transaction, id: Int) {
        val connection: Connection = txn.unbox()
        var ps: PreparedStatement? = null
        try {
            val sql = "DELETE FROM contacts WHERE contactId = ?"
            ps = connection.prepareStatement(sql)
            ps.setInt(1, id)
            val affected = ps.executeUpdate()
            if (affected != 1) throw DbStateException()
            ps.close()
        } catch (e: SQLException) {
            tryToClose(ps, LOG)
            throw DbException(e)
        }
    }

    override fun getContactWithToken(txn: Transaction, token: String): Contact? {
        val connection: Connection = txn.unbox()
        var ps: PreparedStatement? = null
        var rs: ResultSet? = null
        try {
            val sql = """SELECT contactId, inbox, outbox FROM contacts
                                WHERE token = ?
            """.trimIndent()
            ps = connection.prepareStatement(sql)
            ps.setString(1, token)
            rs = ps.executeQuery()
            if (!rs.next()) return null
            val id = rs.getInt(1)
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

    /**
     * Commits a transaction to the database.
     */
    @Throws(DbException::class)
    private fun commitTransaction(txn: Transaction) {
        val connection: Connection = txn.unbox()
        check(!txn.isCommitted)
        txn.setCommitted()
        commitTransaction(connection)
    }

    /**
     * Ends a transaction. If the transaction has not been committed,
     * it will be aborted. If the transaction has been committed,
     * any events attached to the transaction are broadcast.
     * The database lock will be released in either case.
     */
    private fun endTransaction(txn: Transaction) {
        try {
            val connection: Connection = txn.unbox()
            if (!txn.isCommitted) {
                abortTransaction(connection)
            }
        } finally {
            if (txn.isReadOnly) transactionLock.readLock().unlock() else transactionLock.writeLock()
                .unlock()
        }
    }

    override fun <R> read(task: (Transaction) -> R): R {
        return transaction(true, task)
    }

    override fun <R> write(task: (Transaction) -> R): R {
        return transaction(false, task)
    }

    /**
     * Runs the given task within a transaction and returns the result of the
     * task.
     */
    private fun <R> transaction(readOnly: Boolean, task: (Transaction) -> R): R {
        val txn = startTransaction(readOnly)
        try {
            val result = task(txn)
            commitTransaction(txn)
            return result
        } finally {
            endTransaction(txn)
        }
    }

}
