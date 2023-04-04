package org.briarproject.mailbox.android.ui.settings

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.ui.MailboxViewModel
import org.briarproject.mailbox.core.settings.SettingsManager
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_AUTO
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE_MEEK
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE_OBFS4
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE_OBFS4_DEFAULT
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE_SNOWFLAKE
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE_VANILLA
import org.briarproject.mailbox.core.tor.TorConstants.SETTINGS_NAMESPACE
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    private val viewModel: MailboxViewModel by activityViewModels()

    @Inject
    lateinit var settingsManager: SettingsManager
    @Inject
    lateinit var torSettingsStore: TorSettingsStore

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val autoPref = findPreference<SwitchPreferenceCompat>(BRIDGE_AUTO)!!
        val usePref = findPreference<SwitchPreferenceCompat>(BRIDGE_USE)!!
        val snowflakePref = findPreference<SwitchPreferenceCompat>(BRIDGE_USE_SNOWFLAKE)!!
        val meekPref = findPreference<SwitchPreferenceCompat>(BRIDGE_USE_MEEK)!!
        val obfs4Pref = findPreference<SwitchPreferenceCompat>(BRIDGE_USE_OBFS4)!!
        val obfs4DefaultPref = findPreference<SwitchPreferenceCompat>(BRIDGE_USE_OBFS4_DEFAULT)!!
        val vanillaPref = findPreference<SwitchPreferenceCompat>(BRIDGE_USE_VANILLA)!!
        lifecycleScope.launch(Dispatchers.IO) {
            val settings = settingsManager.getSettings(SETTINGS_NAMESPACE)
            withContext(Dispatchers.Main) {
                // TODO get better defaults based on locations
                autoPref.isChecked = settings.getBoolean(BRIDGE_AUTO, true)
                usePref.isChecked = settings.getBoolean(BRIDGE_USE, false)
                snowflakePref.isChecked = settings.getBoolean(BRIDGE_USE_SNOWFLAKE, false)
                meekPref.isChecked = settings.getBoolean(BRIDGE_USE_MEEK, false)
                obfs4Pref.isChecked = settings.getBoolean(BRIDGE_USE_OBFS4, false)
                obfs4DefaultPref.isChecked = settings.getBoolean(BRIDGE_USE_OBFS4_DEFAULT, false)
                vanillaPref.isChecked = settings.getBoolean(BRIDGE_USE_VANILLA, false)
                preferenceManager.preferenceDataStore = torSettingsStore
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onSettingsChanged()
    }
}
