package org.briarproject.mailbox.core

import org.briarproject.mailbox.core.server.WebServerManager
import javax.inject.Inject

@Suppress("unused")
class CoreEagerSingletons @Inject constructor(
    val webServerManager: WebServerManager,
)
