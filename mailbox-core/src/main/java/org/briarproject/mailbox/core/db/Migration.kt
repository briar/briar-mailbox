package org.briarproject.mailbox.core.db

interface Migration<T> {
    /**
     * Returns the schema version from which this migration starts.
     */
    val startVersion: Int

    /**
     * Returns the schema version at which this migration ends.
     */
    val endVersion: Int

    @Throws(DbException::class)
    fun migrate(txn: T)
}
