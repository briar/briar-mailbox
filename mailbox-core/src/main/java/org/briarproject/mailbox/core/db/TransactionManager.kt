package org.briarproject.mailbox.core.db

interface TransactionManager {

    /**
     * Runs the given task within a transaction.
     */
    @Throws(DbException::class)
    fun transaction(readOnly: Boolean, task: (Transaction) -> Unit)

    /**
     * Runs the given task within a transaction and returns the result of the
     * task.
     */
    @Throws(DbException::class)
    fun <R> transactionWithResult(readOnly: Boolean, task: (Transaction) -> R): R
}
