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
import org.briarproject.mailbox.core.settings.Settings
import org.briarproject.mailbox.core.settings.SettingsManager
import org.briarproject.mailbox.core.system.LocationUtils
import org.briarproject.mailbox.core.tor.NetworkManager
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_AUTO
import org.briarproject.mailbox.core.tor.TorConstants.BRIDGE_AUTO_DEFAULT
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

    private lateinit var autoPref: SwitchPreferenceCompat
    private lateinit var usePref: SwitchPreferenceCompat
    private lateinit var snowflakePref: SwitchPreferenceCompat
    private lateinit var meekPref: SwitchPreferenceCompat
    private lateinit var obfs4Pref: SwitchPreferenceCompat
    private lateinit var obfs4DefaultPref: SwitchPreferenceCompat
    private lateinit var vanillaPref: SwitchPreferenceCompat
    private lateinit var brideTypePrefs: List<Preference>
    private lateinit var bridgeTypesCategory: PreferenceCategory

    @Volatile
    private var settings: Settings? = null

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

    @Inject
    lateinit var networkManager: NetworkManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        autoPref = findPreference(BRIDGE_AUTO)!!
        usePref = findPreference(BRIDGE_USE)!!
        snowflakePref = findPreference(BRIDGE_USE_SNOWFLAKE)!!
        meekPref = findPreference(BRIDGE_USE_MEEK)!!
        obfs4Pref = findPreference(BRIDGE_USE_OBFS4)!!
        obfs4DefaultPref = findPreference(BRIDGE_USE_OBFS4_DEFAULT)!!
        vanillaPref = findPreference(BRIDGE_USE_VANILLA)!!
        brideTypePrefs = listOf(
            snowflakePref, meekPref, obfs4Pref, obfs4DefaultPref, vanillaPref
        )
        bridgeTypesCategory = findPreference("bridgeTypesCategory")!!
        autoPref.setOnPreferenceChangeListener { _, newValue ->
            onAutoChanged(newValue as Boolean)
            true
        }
        usePref.setOnPreferenceChangeListener { _, newValue ->
            onUseBridgesChanged(newValue as Boolean)
            true
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val settings = settingsManager.getSettings(SETTINGS_NAMESPACE)
            this@SettingsFragment.settings = settings
            withContext(Dispatchers.Main) {
                onAutoChanged(settings.getBoolean(BRIDGE_AUTO, BRIDGE_AUTO_DEFAULT))
                preferenceManager.preferenceDataStore = torSettingsStore
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // apply settings only when user is leaving settings to prevent Tor changes on each toggle
        viewModel.onSettingsChanged()
    }

    private fun onAutoChanged(auto: Boolean) {
        autoPref.isChecked = auto
        val country = locationUtils.currentCountry
        val doBridgesWork = circumventionProvider.doBridgesWork(country)
        // if automatic mode is on, we show what Tor is using, otherwise we show what user has set
        if (auto) {
            usePref.isChecked = doBridgesWork
            onUseBridgesChanged(doBridgesWork)
            val autoTypes = if (networkManager.networkStatus.isIpv6Only) {
                listOf(MEEK, SNOWFLAKE)
            } else {
                circumventionProvider.getSuitableBridgeTypes(country)
            }
            circumventionProvider.getSuitableBridgeTypes(country)
            snowflakePref.isChecked = autoTypes.contains(SNOWFLAKE)
            meekPref.isChecked = autoTypes.contains(MEEK)
            obfs4Pref.isChecked = autoTypes.contains(NON_DEFAULT_OBFS4)
            obfs4DefaultPref.isChecked = autoTypes.contains(DEFAULT_OBFS4)
            vanillaPref.isChecked = autoTypes.contains(VANILLA)
        } else {
            val settings = this.settings ?: return
            val useBridges = settings.getBoolean(BRIDGE_USE, doBridgesWork)
            usePref.isChecked = useBridges
            onUseBridgesChanged(useBridges)
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
        }
    }

    private fun onUseBridgesChanged(useBridges: Boolean) {
        brideTypePrefs.forEach { it.isVisible = useBridges }
        bridgeTypesCategory.isVisible = useBridges
    }
}
