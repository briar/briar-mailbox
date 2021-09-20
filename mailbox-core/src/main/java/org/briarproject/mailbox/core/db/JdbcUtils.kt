package org.briarproject.mailbox.core.db

import org.briarproject.mailbox.core.util.LogUtils.logException
import org.slf4j.Logger
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

object JdbcUtils {

    fun tryToClose(rs: ResultSet?, logger: Logger) {
        try {
            rs?.close()
        } catch (e: SQLException) {
            logException(logger, e)
        }
    }

    fun tryToClose(s: Statement?, logger: Logger) {
        try {
            s?.close()
        } catch (e: SQLException) {
            logException(logger, e)
        }
    }

    fun tryToClose(c: Connection?, logger: Logger) {
        try {
            c?.close()
        } catch (e: SQLException) {
            logException(logger, e)
        }
    }

}
