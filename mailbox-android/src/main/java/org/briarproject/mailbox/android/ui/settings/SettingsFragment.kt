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
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.ui.MailboxViewModel
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
        torSettingsStore.setOnSettingsLoadedCallback { settings ->
            lifecycleScope.launch(Dispatchers.Main) {
                onAutoChanged(settings.getBoolean(BRIDGE_AUTO, BRIDGE_AUTO_DEFAULT))
                // we set the store for persistence of settings only after setting up auto value
                preferenceManager.preferenceDataStore = torSettingsStore
            }
        }
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
            setIsPersistent(false)
            usePref.isChecked = doBridgesWork
            onUseBridgesChanged(doBridgesWork)
            val autoTypes = if (networkManager.networkStatus.isIpv6Only) {
                listOf(MEEK, SNOWFLAKE)
            } else {
                circumventionProvider.getSuitableBridgeTypes(country)
            }
            snowflakePref.isChecked = autoTypes.contains(SNOWFLAKE)
            meekPref.isChecked = autoTypes.contains(MEEK)
            obfs4Pref.isChecked = autoTypes.contains(NON_DEFAULT_OBFS4)
            obfs4DefaultPref.isChecked = autoTypes.contains(DEFAULT_OBFS4)
            vanillaPref.isChecked = autoTypes.contains(VANILLA)
        } else {
            setIsPersistent(true)
            val useBridges =
                torSettingsStore.getBooleanAndStoreDefault(usePref, BRIDGE_USE, doBridgesWork)
            onUseBridgesChanged(useBridges)
            val customTypes = torPlugin.customBridgeTypes
            torSettingsStore.getBooleanAndStoreDefault(
                pref = snowflakePref,
                key = BRIDGE_USE_SNOWFLAKE,
                defaultValue = customTypes.contains(SNOWFLAKE)
            )
            torSettingsStore.getBooleanAndStoreDefault(
                pref = meekPref,
                key = BRIDGE_USE_MEEK,
                defaultValue = customTypes.contains(MEEK),
            )
            torSettingsStore.getBooleanAndStoreDefault(
                pref = obfs4Pref,
                key = BRIDGE_USE_OBFS4,
                defaultValue = customTypes.contains(NON_DEFAULT_OBFS4),
            )
            torSettingsStore.getBooleanAndStoreDefault(
                pref = obfs4DefaultPref,
                key = BRIDGE_USE_OBFS4_DEFAULT,
                defaultValue = customTypes.contains(DEFAULT_OBFS4),
            )
            torSettingsStore.getBooleanAndStoreDefault(
                pref = vanillaPref,
                key = BRIDGE_USE_VANILLA,
                defaultValue = customTypes.contains(VANILLA),
            )
        }
    }

    private fun onUseBridgesChanged(useBridges: Boolean) {
        brideTypePrefs.forEach { it.isVisible = useBridges }
        bridgeTypesCategory.isVisible = useBridges
    }

    private fun setIsPersistent(enable: Boolean) {
        usePref.isPersistent = enable
        brideTypePrefs.forEach { pref -> pref.isPersistent = enable }
    }
}
