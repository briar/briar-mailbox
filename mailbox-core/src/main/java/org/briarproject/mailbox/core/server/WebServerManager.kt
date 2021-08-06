package org.briarproject.mailbox.core.server

import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.briarproject.mailbox.core.lifecycle.Service
import java.util.logging.Logger.getLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebServerManager @Inject constructor() : Service {

    internal companion object {
        private const val PORT = 8888
        private val LOG = getLogger(WebServerManager::class.java.name)
    }

    private val server by lazy {
        embeddedServer(Netty, PORT, watchPaths = emptyList()) {
            install(CallLogging)
            configureRouting()
        }
    }

    override fun startService() {
        // hangs if not starting inside a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            LOG.info("starting")
            server.start(wait = true)
            LOG.info("started")
        }
    }

    override fun stopService() {
        server.stop(1_000, 2_000)
    }

}
