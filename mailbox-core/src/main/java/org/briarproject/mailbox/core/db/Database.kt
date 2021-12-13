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
import org.briarproject.mailbox.core.settings.Settings

interface Database : TransactionManager {

    /**
     * Opens the database and returns true if the database already existed. Existence of the
     * database is defined as the database files exists and the database contains a valid schema.
     */
    fun open(listener: MigrationListener?): Boolean

    /**
     * Prevents new transactions from starting, waits for all current
     * transactions to finish, and closes the database.
     */
    @Throws(DbException::class)
    fun close()

    @Throws(DbException::class)
    fun dropAllTablesAndClose()

    @Throws(DbException::class)
    fun getSettings(txn: Transaction, namespace: String): Settings

    @Throws(DbException::class)
    fun mergeSettings(txn: Transaction, s: Settings, namespace: String)

    /**
     * Adds a contact to the database. It is the callers responsibility to use [getContact] before
     * to check if a contact with the same ID already exists.
     */
    @Throws(DbException::class)
    fun addContact(txn: Transaction, contact: Contact)

    @Throws(DbException::class)
    fun getContact(txn: Transaction, id: Int): Contact?

    @Throws(DbException::class)
    fun getContacts(txn: Transaction): List<Contact>

    @Throws(DbException::class)
    fun removeContact(txn: Transaction, id: Int)

    @Throws(DbException::class)
    fun getContactWithToken(txn: Transaction, token: String): Contact?

}
