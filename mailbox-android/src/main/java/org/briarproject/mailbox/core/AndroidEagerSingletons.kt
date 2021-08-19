package org.briarproject.mailbox.core

import org.briarproject.mailbox.core.tor.AndroidTorPlugin
import javax.inject.Inject

@Suppress("unused")
internal class AndroidEagerSingletons @Inject constructor(
    val androidTorPlugin: AndroidTorPlugin,
)
