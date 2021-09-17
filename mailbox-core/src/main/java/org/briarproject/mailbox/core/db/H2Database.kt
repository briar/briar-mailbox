package org.briarproject.mailbox.core.db

import org.briarproject.mailbox.core.db.JdbcUtils.tryToClose
import org.briarproject.mailbox.core.system.Clock
import org.briarproject.mailbox.core.util.IoUtils.isNonEmptyDirectory
import org.briarproject.mailbox.core.util.LogUtils.logFileOrDir
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement
import java.util.Properties

class H2Database(
    private val config: DatabaseConfig,
    val clock: Clock,
) : JdbcDatabase(dbTypes, clock) {

    internal companion object {
        private val LOG = LoggerFactory.getLogger(H2Database::class.java)

        private const val HASH_TYPE = "BINARY(32)"
        private const val SECRET_TYPE = "BINARY(32)"
        private const val BINARY_TYPE = "BINARY"
        private const val COUNTER_TYPE = "INT NOT NULL AUTO_INCREMENT"
        private const val STRING_TYPE = "VARCHAR"
        private val dbTypes = DatabaseTypes(
            HASH_TYPE, SECRET_TYPE, BINARY_TYPE, COUNTER_TYPE, STRING_TYPE
        )
    }

    private val dbPath: String get() = File(config.getDatabaseDirectory(), "db").absolutePath
    private val url: String = ("jdbc:h2:split:$dbPath;MULTI_THREADED=1;WRITE_DELAY=0")

    override fun open(listener: MigrationListener?): Boolean {
        val dir = config.getDatabaseDirectory()
        if (LOG.isInfoEnabled) {
            LOG.info("Contents of account directory before opening DB:")
            logFileOrDir(LOG, dir.parentFile)
        }
        val reopen = isNonEmptyDirectory(dir)
        if (LOG.isInfoEnabled) LOG.info("Reopening DB: $reopen")
        if (!reopen && dir.mkdirs()) LOG.info("Created database directory")
        super.open("org.h2.Driver", reopen, listener)
        if (LOG.isInfoEnabled) {
            LOG.info("Contents of account directory after opening DB:")
            logFileOrDir(LOG, dir.parentFile)
        }
        return reopen
    }

    override fun close() {
        // H2 will close the database when the last connection closes
        var c: Connection? = null
        try {
            c = createConnection()
            super.closeAllConnections()
            setDirty(c, false)
            c.close()
        } catch (e: SQLException) {
            tryToClose(c, LOG)
            throw DbException(e)
        }
    }

    @Throws(DbException::class, SQLException::class)
    override fun createConnection(): Connection {
        val props = Properties()
        return DriverManager.getConnection(url, props)
    }

    override fun compactAndClose() {
        var c: Connection? = null
        var s: Statement? = null
        try {
            c = createConnection()
            closeAllConnections()
            s = c.createStatement()
            s.execute("SHUTDOWN COMPACT")
            s.close()
            c.close()
        } catch (e: SQLException) {
            tryToClose(s, LOG)
            tryToClose(c, LOG)
            throw DbException(e)
        }
    }

}
