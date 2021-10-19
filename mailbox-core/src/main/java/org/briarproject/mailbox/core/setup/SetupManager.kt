package org.briarproject.mailbox.core.setup

import org.briarproject.mailbox.core.db.DbException
import org.briarproject.mailbox.core.db.Transaction
import org.briarproject.mailbox.core.settings.Settings
import org.briarproject.mailbox.core.settings.SettingsManager
import org.briarproject.mailbox.core.system.RandomIdManager
import javax.inject.Inject

private const val SETTINGS_NAMESPACE_OWNER = "owner"
private const val SETTINGS_SETUP_TOKEN = "setupToken"
private const val SETTINGS_OWNER_TOKEN = "ownerToken"

class SetupManager @Inject constructor(
    private val randomIdManager: RandomIdManager,
    private val settingsManager: SettingsManager,
) {

    /**
     * Stores a new single-use setup token and wipes the owner auth token, if one existed.
     */
    fun restartSetup() {
        val settings = Settings()
        settings[SETTINGS_SETUP_TOKEN] = randomIdManager.getNewRandomId()
        settings[SETTINGS_OWNER_TOKEN] = "" // we can't remove or null or, so we need to empty it
        settingsManager.mergeSettings(settings, SETTINGS_NAMESPACE_OWNER)
    }

    /**
     * Visible for testing, consider private.
     */
    @Throws(DbException::class)
    internal fun setOwnerToken(token: String) {
        val settings = Settings()
        settings[SETTINGS_OWNER_TOKEN] = token
        settingsManager.mergeSettings(settings, SETTINGS_NAMESPACE_OWNER)
    }

    @Throws(DbException::class)
    fun getOwnerToken(txn: Transaction): String? {
        val settings = settingsManager.getSettings(txn, SETTINGS_NAMESPACE_OWNER)
        val ownerToken = settings[SETTINGS_OWNER_TOKEN]
        return if (ownerToken.isNullOrEmpty()) null
        else ownerToken
    }

}
