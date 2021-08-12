package org.briarproject.mailbox.core

import org.briarproject.mailbox.core.tor.JavaTorPlugin
import javax.inject.Inject

@Suppress("unused")
internal class JavaCliEagerSingletons @Inject constructor(
    val javaTorPlugin: JavaTorPlugin,
)
