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
            logException(logger, e) { "Error while closing result set" }
        }
    }

    fun tryToClose(s: Statement?, logger: Logger) {
        try {
            s?.close()
        } catch (e: SQLException) {
            logException(logger, e) { "Error while closing statement" }
        }
    }

    fun tryToClose(c: Connection?, logger: Logger) {
        try {
            c?.close()
        } catch (e: SQLException) {
            logException(logger, e) { "Error while closing connection" }
        }
    }

}
