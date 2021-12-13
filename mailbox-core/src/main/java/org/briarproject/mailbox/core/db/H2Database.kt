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

import org.briarproject.mailbox.core.db.JdbcUtils.tryToClose
import org.briarproject.mailbox.core.system.Clock
import org.briarproject.mailbox.core.util.IoUtils.isNonEmptyDirectory
import org.briarproject.mailbox.core.util.LogUtils.info
import org.briarproject.mailbox.core.util.LogUtils.logFileOrDir
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.util.Properties

open class H2Database(
    private val config: DatabaseConfig,
    clock: Clock,
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
    private val url: String = ("jdbc:h2:split:$dbPath;WRITE_DELAY=0")

    override fun open(listener: MigrationListener?): Boolean {
        val dir = config.getDatabaseDirectory()
        LOG.info { "Contents of account directory before opening DB:" }
        logFileOrDir(LOG, dir.parentFile)
        val databaseDirNonEmpty = isNonEmptyDirectory(dir)
        if (!databaseDirNonEmpty && dir.mkdirs()) LOG.info("Created database directory")
        val reopen = super.open("org.h2.Driver", listener)
        LOG.info { "Contents of account directory after opening DB:" }
        logFileOrDir(LOG, dir.parentFile)
        return reopen
    }

    override fun databaseHasSettingsTable(): Boolean {
        return read { txn ->
            val connection = txn.unbox()
            var tables: ResultSet? = null
            try {
                // Need to check for PUBLIC schema as there is another table called SETTINGS on the
                // INFORMATION_SCHEMA schema.
                tables = connection.metaData.getTables(null, "PUBLIC", "SETTINGS", null)
                // if that query returns any rows, the settings table does exist
                tables.next()
            } catch (e: SQLException) {
                LOG.warn("Error while checking for settings table existence", e)
                tryToClose(tables, LOG)
                false
            }
        }
    }

    override fun close() {
        connectionsLock.lock()
        try {
            // This extra check is mainly added for tests where we might have closed the database
            // already by resetting the database after each test and then the lifecycle manager
            // tries to close again. However closing an already closed database doesn't make
            // sense, also in production, so bail out quickly here.
            // This is important especially after the database has been cleared, because the
            // settings table is gone and if we allowed the flow to continue further, we would try
            // to store the dirty flag in the no longer existing settings table.
            if (closed) return
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
        } finally {
            connectionsLock.unlock()
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
