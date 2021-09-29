package org.briarproject.mailbox.core.db

import org.briarproject.mailbox.core.api.Contact
import org.briarproject.mailbox.core.settings.Settings
import java.sql.Connection

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

    /**
     * Aborts the given transaction - no changes made during the transaction
     * will be applied to the database.
     */
    fun abortTransaction(connection: Connection)

    /**
     * Commits the given transaction - all changes made during the transaction
     * will be applied to the database.
     */
    @Throws(DbException::class)
    fun commitTransaction(connection: Connection)

    @Throws(DbException::class)
    fun getSettings(txn: Transaction, namespace: String): Settings

    @Throws(DbException::class)
    fun mergeSettings(txn: Transaction, s: Settings, namespace: String)

    @Throws(DbException::class)
    fun addContact(txn: Transaction, contact: Contact)

    @Throws(DbException::class)
    fun getContact(txn: Transaction, id: Int): Contact?

    @Throws(DbException::class)
    fun removeContact(txn: Transaction, id: Int)

}
