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
     * Returns the database transaction. The type of the returned object
     * depends on the database implementation.
     */
    fun unbox(): Connection {
        return txn
    }

    /**
     * Marks the transaction as committed. This method should only be called
     * by the DatabaseComponent. It must not be called more than once.
     */
    fun setCommitted() {
        check(!isCommitted)
        isCommitted = true
    }
}
