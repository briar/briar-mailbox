package org.briarproject.mailbox.core.tor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class FakeTorPlugin @Inject constructor() : TorPlugin {

    private val state = MutableStateFlow<TorState>(TorState.StartingStopping)

    override fun startService() {
        state.value = TorState.Published
    }

    override fun stopService() {
        state.value = TorState.StartingStopping
    }

    override fun getState(): StateFlow<TorState> {
        return state
    }

    override fun getHiddenServiceAddress(): String? = null
}
