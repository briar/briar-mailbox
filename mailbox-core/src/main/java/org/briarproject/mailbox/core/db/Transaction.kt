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

import java.sql.Connection

class Transaction(
    private val txn: Connection,
    /**
     * Returns true if the transaction can only be used for reading.
     */
    val isReadOnly: Boolean,
) {

    /**
     * Returns true if the transaction has been committed.
     */
    var isCommitted = false
        private set

    /**
     * Returns the database connection.
     */
    fun unbox(): Connection {
        return txn
    }

    /**
     * Marks the transaction as committed. This method should only be called
     * by the Database. It must not be called more than once.
     */
    fun setCommitted() {
        check(!isCommitted)
        isCommitted = true
    }
}
