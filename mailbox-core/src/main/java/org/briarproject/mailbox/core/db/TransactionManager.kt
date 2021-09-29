package org.briarproject.mailbox.core.db

interface TransactionManager {
    /**
     * Starts a new transaction and returns an object representing it.
     *
     *
     * This method acquires locks, so it must not be called while holding a
     * lock.
     *
     * @param readOnly true if the transaction will only be used for reading.
     */
    @Throws(DbException::class)
    fun startTransaction(readOnly: Boolean): Transaction

    /**
     * Commits a transaction to the database.
     */
    @Throws(DbException::class)
    fun commitTransaction(txn: Transaction)

    /**
     * Ends a transaction. If the transaction has not been committed,
     * it will be aborted. If the transaction has been committed,
     * any events attached to the transaction are broadcast.
     * The database lock will be released in either case.
     */
    fun endTransaction(txn: Transaction)

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
