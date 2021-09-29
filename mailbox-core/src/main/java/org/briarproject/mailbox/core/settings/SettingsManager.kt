package org.briarproject.mailbox.core.settings

import org.briarproject.mailbox.core.db.DbException
import org.briarproject.mailbox.core.db.Transaction

interface SettingsManager {
    /**
     * Returns all settings in the given namespace.
     */
    @Throws(DbException::class)
    fun getSettings(namespace: String): Settings

    /**
     * Returns all settings in the given namespace.
     */
    @Throws(DbException::class)
    fun getSettings(txn: Transaction, namespace: String): Settings

    /**
     * Merges the given settings with any existing settings in the given
     * namespace.
     */
    @Throws(DbException::class)
    fun mergeSettings(s: Settings, namespace: String)
}
