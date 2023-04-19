package org.briarproject.mailbox.core.tor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.briarproject.onionwrapper.CircumventionProvider.BridgeType
import javax.inject.Inject

class FakeTorPlugin @Inject constructor() : TorPlugin {

    private val state = MutableStateFlow<TorPluginState>(TorPluginState.StartingStopping)

    override fun startService() {
        state.value = TorPluginState.Published
    }

    override fun stopService() {
        state.value = TorPluginState.StartingStopping
    }

    override fun getState(): StateFlow<TorPluginState> {
        return state
    }

    override fun onSettingsChanged() {
    }

    override fun getHiddenServiceAddress(): String? = null
    override fun getCustomBridgeTypes(): List<BridgeType> = emptyList()
}
