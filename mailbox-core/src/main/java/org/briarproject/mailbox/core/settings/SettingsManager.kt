package org.briarproject.mailbox.core.settings

import org.briarproject.mailbox.core.db.DbException
import java.sql.Connection

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
    fun getSettings(txn: Connection, namespace: String): Settings

    /**
     * Merges the given settings with any existing settings in the given
     * namespace.
     */
    @Throws(DbException::class)
    fun mergeSettings(s: Settings, namespace: String)
}
