package org.briarproject.mailbox.core

import org.briarproject.mailbox.core.system.AndroidTaskScheduler
import org.briarproject.mailbox.core.system.DozeWatchdog
import org.briarproject.mailbox.core.tor.AndroidNetworkManager
import org.briarproject.mailbox.core.tor.TorPlugin
import javax.inject.Inject

@Suppress("unused")
internal class AndroidEagerSingletons @Inject constructor(
    val androidTaskScheduler: AndroidTaskScheduler,
    val androidNetworkManager: AndroidNetworkManager,
    val androidTorPlugin: TorPlugin,
    val dozeWatchdog: DozeWatchdog,
)
