package org.briarproject.mailbox.android.ui.settings

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.ui.MailboxViewModel
import org.briarproject.mailbox.core.settings.SettingsManager
import org.briarproject.mailbox.core.system.LocationUtils
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_AUTO
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE_MEEK
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE_OBFS4
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE_OBFS4_DEFAULT
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE_SNOWFLAKE
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_USE_VANILLA
import org.briarproject.mailbox.core.tor.TorConstants.SETTINGS_NAMESPACE
import org.briarproject.mailbox.core.tor.TorPlugin
import org.briarproject.onionwrapper.CircumventionProvider
import org.briarproject.onionwrapper.CircumventionProvider.BridgeType.DEFAULT_OBFS4
import org.briarproject.onionwrapper.CircumventionProvider.BridgeType.MEEK
import org.briarproject.onionwrapper.CircumventionProvider.BridgeType.NON_DEFAULT_OBFS4
import org.briarproject.onionwrapper.CircumventionProvider.BridgeType.SNOWFLAKE
import org.briarproject.onionwrapper.CircumventionProvider.BridgeType.VANILLA
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    private val viewModel: MailboxViewModel by activityViewModels()
    private lateinit var brideTypePrefs: List<Preference>
    private lateinit var bridgeTypesCategory: PreferenceCategory

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var torSettingsStore: TorSettingsStore

    @Inject
    lateinit var torPlugin: TorPlugin

    @Inject
    lateinit var locationUtils: LocationUtils

    @Inject
    lateinit var circumventionProvider: CircumventionProvider

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val autoPref = findPreference<SwitchPreferenceCompat>(BRIDGE_AUTO)!!
        val usePref = findPreference<SwitchPreferenceCompat>(BRIDGE_USE)!!
        val snowflakePref = findPreference<SwitchPreferenceCompat>(BRIDGE_USE_SNOWFLAKE)!!
        val meekPref = findPreference<SwitchPreferenceCompat>(BRIDGE_USE_MEEK)!!
        val obfs4Pref = findPreference<SwitchPreferenceCompat>(BRIDGE_USE_OBFS4)!!
        val obfs4DefaultPref = findPreference<SwitchPreferenceCompat>(BRIDGE_USE_OBFS4_DEFAULT)!!
        val vanillaPref = findPreference<SwitchPreferenceCompat>(BRIDGE_USE_VANILLA)!!
        brideTypePrefs = listOf(
            snowflakePref, meekPref, obfs4Pref, obfs4DefaultPref, vanillaPref
        )
        bridgeTypesCategory = findPreference("bridgeTypesCategory")!!
        usePref.setOnPreferenceChangeListener { _, newValue ->
            onUseBridgesChanges(newValue as Boolean)
            true
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val settings = settingsManager.getSettings(SETTINGS_NAMESPACE)
            withContext(Dispatchers.Main) {
                autoPref.isChecked = settings.getBoolean(BRIDGE_AUTO, true)
                val country = locationUtils.currentCountry
                val doBridgesWork = circumventionProvider.doBridgesWork(country)
                val useBridges = settings.getBoolean(BRIDGE_USE, doBridgesWork)
                usePref.isChecked = useBridges
                onUseBridgesChanges(useBridges)
                val customTypes = torPlugin.customBridgeTypes
                snowflakePref.isChecked = settings.getBoolean(
                    key = BRIDGE_USE_SNOWFLAKE,
                    defaultValue = customTypes.contains(SNOWFLAKE),
                )
                meekPref.isChecked = settings.getBoolean(
                    key = BRIDGE_USE_MEEK,
                    defaultValue = customTypes.contains(MEEK),
                )
                obfs4Pref.isChecked = settings.getBoolean(
                    key = BRIDGE_USE_OBFS4,
                    defaultValue = customTypes.contains(NON_DEFAULT_OBFS4),
                )
                obfs4DefaultPref.isChecked = settings.getBoolean(
                    key = BRIDGE_USE_OBFS4_DEFAULT,
                    defaultValue = customTypes.contains(DEFAULT_OBFS4),
                )
                vanillaPref.isChecked = settings.getBoolean(
                    key = BRIDGE_USE_VANILLA,
                    defaultValue = customTypes.contains(VANILLA),
                )
                preferenceManager.preferenceDataStore = torSettingsStore
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // apply settings only when user is leaving settings to prevent Tor changes on each toggle
        viewModel.onSettingsChanged()
    }

    private fun onUseBridgesChanges(useBridges: Boolean) {
        brideTypePrefs.forEach { it.isVisible = useBridges }
        bridgeTypesCategory.isVisible = useBridges
    }
}
