package org.briarproject.mailbox.core.db

import org.briarproject.mailbox.core.contacts.Contact
import org.briarproject.mailbox.core.settings.Settings

interface Database : TransactionManager {

    /**
     * Opens the database and returns true if the database already existed.
     */
    fun open(listener: MigrationListener?): Boolean

    /**
     * Prevents new transactions from starting, waits for all current
     * transactions to finish, and closes the database.
     */
    @Throws(DbException::class)
    fun close()

    @Throws(DbException::class)
    fun clearDatabase(txn: Transaction)

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
