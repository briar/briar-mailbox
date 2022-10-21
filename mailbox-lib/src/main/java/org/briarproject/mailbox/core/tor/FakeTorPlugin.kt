package org.briarproject.mailbox.core.tor

import org.briarproject.mailbox.core.lifecycle.IoExecutor
import org.briarproject.mailbox.core.settings.SettingsManager
import org.briarproject.mailbox.core.system.Clock
import org.briarproject.mailbox.core.system.LocationUtils
import org.briarproject.mailbox.core.system.ResourceProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.Executor
import javax.inject.Inject

class FakeTorPlugin @Inject internal constructor(
    @IoExecutor ioExecutor: Executor,
    settingsManager: SettingsManager,
    clock: Clock,
    circumventionProvider: CircumventionProvider,
) : TorPlugin(
    ioExecutor,
    settingsManager,
    NetworkManager { NetworkStatus(false, false, false) },
    LocationUtils { "US" },
    clock,
    ResourceProvider { _, _ -> ByteArrayInputStream(byteArrayOf(0x00)) },
    circumventionProvider,
    null,
    File(""),
) {
    override fun startService() {
        state.setStarted()
        state.enableNetwork(true)
        circuitStatus("BUILT", "", "")
        orConnStatus("CONNECTED", "")
        state.setBootstrapPercent(100)
        for (i in 1..5) state.onServiceDescriptorUploaded()
    }

    override fun stopService() {}
    override fun getProcessId(): Int = 0
    override fun getLastUpdateTime(): Long = Long.MAX_VALUE
}
