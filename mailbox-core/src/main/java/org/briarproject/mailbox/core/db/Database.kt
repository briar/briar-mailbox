package org.briarproject.mailbox.core.db

import org.briarproject.mailbox.core.api.Contact
import org.briarproject.mailbox.core.settings.Settings
import java.sql.Connection

interface Database {

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
     * Starts a new transaction and returns an object representing it.
     */
    @Throws(DbException::class)
    fun startTransaction(): Connection

    /**
     * Aborts the given transaction - no changes made during the transaction
     * will be applied to the database.
     */
    fun abortTransaction(txn: Connection)

    /**
     * Commits the given transaction - all changes made during the transaction
     * will be applied to the database.
     */
    @Throws(DbException::class)
    fun commitTransaction(txn: Connection)

    @Throws(DbException::class)
    fun getSettings(txn: Connection, namespace: String?): Settings

    @Throws(DbException::class)
    fun mergeSettings(txn: Connection, s: Settings, namespace: String?)

    @Throws(DbException::class)
    fun addContact(txn: Connection, contact: Contact)

    @Throws(DbException::class)
    fun getContact(txn: Connection, id: Int): Contact?

    @Throws(DbException::class)
    fun removeContact(txn: Connection, id: Int)

}
