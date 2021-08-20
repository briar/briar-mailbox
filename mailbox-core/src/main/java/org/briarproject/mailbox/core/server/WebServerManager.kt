package org.briarproject.mailbox.core.server

import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.briarproject.mailbox.core.lifecycle.Service
import org.slf4j.LoggerFactory.getLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebServerManager @Inject constructor() : Service {

    internal companion object {
        internal const val PORT = 8000
        private val LOG = getLogger(WebServerManager::class.java)
    }

    private val server by lazy {
        embeddedServer(Netty, PORT, watchPaths = emptyList()) {
            install(CallLogging)
            configureRouting()
        }
    }

    override fun startService() {
        LOG.info("starting")
        server.start()
        LOG.info("started")
    }

    override fun stopService() {
        server.stop(1_000, 2_000)
    }

}
