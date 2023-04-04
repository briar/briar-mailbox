package org.briarproject.mailbox.android.ui.settings

import androidx.preference.PreferenceDataStore
import org.briarproject.mailbox.core.db.DbException
import org.briarproject.mailbox.core.lifecycle.IoExecutor
import org.briarproject.mailbox.core.settings.Settings
import org.briarproject.mailbox.core.settings.SettingsManager
import org.briarproject.mailbox.core.tor.TorConstants.SETTINGS_NAMESPACE
import org.briarproject.mailbox.core.util.LogUtils.info
import org.briarproject.mailbox.core.util.LogUtils.logDuration
import org.briarproject.mailbox.core.util.LogUtils.logException
import org.briarproject.mailbox.core.util.LogUtils.now
import org.slf4j.LoggerFactory.getLogger
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

private val LOG = getLogger(SettingsStore::class.java)

@Singleton
class TorSettingsStore @Inject constructor(
    settingsManager: SettingsManager,
    @IoExecutor
    ioExecutor: Executor,
) : SettingsStore(settingsManager, ioExecutor, SETTINGS_NAMESPACE)

/**
 * This is only for storing settings. We still need to retrieve the current value ourselves.
 */
open class SettingsStore(
    private val settingsManager: SettingsManager,
    private val dbExecutor: Executor,
    private val namespace: String,
) : PreferenceDataStore() {

    override fun putBoolean(key: String, value: Boolean) {
        LOG.info { "Store bool setting: $key=$value" }
        val s = Settings().apply {
            putBoolean(key, value)
        }
        storeSettings(s)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return settingsManager.getSettings(SETTINGS_NAMESPACE).getBoolean(key, defValue)
    }

    private fun storeSettings(s: Settings) {
        dbExecutor.execute {
            try {
                val start: Long = now()
                settingsManager.mergeSettings(s, namespace)
                logDuration(LOG, start) { "Merging $namespace settings" }
            } catch (e: DbException) {
                logException(LOG, e, "Error storing settings: ")
            }
        }
    }
}
