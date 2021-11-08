package org.briarproject.mailbox.core.db

interface TransactionManager {

    /**
     * Runs the given task within a read-only transaction and returns its result.
     */
    @Throws(DbException::class)
    fun <R> read(task: (Transaction) -> R): R

    /**
     * Runs the given task within a read/write transaction and returns its result.
     */
    @Throws(DbException::class)
    fun <R> write(task: (Transaction) -> R): R

}
