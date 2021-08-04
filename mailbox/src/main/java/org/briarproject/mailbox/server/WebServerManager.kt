package org.briarproject.mailbox.server

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.logging.Logger.getLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WebServerManager @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {

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

    fun start() {
        // hangs if not starting inside a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            LOG.info("starting")
            server.start(wait = true)
            LOG.info("started")
        }
    }

    fun stop() {
        server.stop(1_000, 2_000)
    }

}
