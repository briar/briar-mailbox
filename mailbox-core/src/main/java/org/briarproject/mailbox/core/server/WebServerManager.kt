package org.briarproject.mailbox.core.server

import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.briarproject.mailbox.core.contacts.ContactsManager
import org.briarproject.mailbox.core.files.FileManager
import org.briarproject.mailbox.core.lifecycle.Service
import org.briarproject.mailbox.core.server.WebServerManager.Companion.PORT
import org.briarproject.mailbox.core.settings.MetadataRouteManager
import org.briarproject.mailbox.core.setup.SetupRouteManager
import org.briarproject.mailbox.core.setup.WipeManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import javax.inject.Inject
import javax.inject.Singleton

interface WebServerManager : Service {
    companion object {
        const val PORT: Int = 8000
    }
}

@Singleton
internal class WebServerManagerImpl @Inject constructor(
    private val authManager: AuthManager,
    private val metadataRouteManager: MetadataRouteManager,
    private val setupRouteManager: SetupRouteManager,
    private val contactsManager: ContactsManager,
    private val fileManager: FileManager,
    private val wipeManager: WipeManager,
) : WebServerManager {

    internal companion object {
        private val LOG: Logger = getLogger(WebServerManager::class.java)
    }

    private val server by lazy {
        embeddedServer(Netty, PORT, watchPaths = emptyList()) {
            install(CallLogging)
            install(Authentication) {
                bearer {
                    authenticationFunction = { token ->
                        // TODO: Remove logging of token before release.
                        LOG.error("token: $token")
                        authManager.getPrincipal(token)
                    }
                }
            }
            install(ContentNegotiation) {
                jackson()
            }
            configureBasicApi(metadataRouteManager, setupRouteManager, wipeManager)
            configureContactApi(contactsManager)
            configureFilesApi(fileManager)
        }
    }

    override fun startService() {
        server.start()
    }

    override fun stopService() {
        server.stop(1_000, 2_000)
    }

}
