package org.briarproject.mailbox.core.server

import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.features.CallLogging
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.briarproject.mailbox.core.lifecycle.Service
import org.briarproject.mailbox.core.server.WebServerManager.Companion.PORT
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
    private val authManager: AuthenticationManager,
) : WebServerManager {

    internal companion object {
        private val LOG: Logger = getLogger(WebServerManager::class.java)
    }

    private val server by lazy {
        embeddedServer(Netty, PORT, watchPaths = emptyList()) {
            install(CallLogging)
            // TODO validate folderId and fileId somewhere
            install(Authentication) {
                bearer(AuthContext.ownerOnly) {
                    realm = "Briar Mailbox Owner"
                    authenticationFunction = { credentials ->
                        // TODO: Remove logging [of credentials] before release.
                        LOG.error("credentials: $credentials")
                        if (authManager.canOwnerAccess(credentials)) {
                            UserIdPrincipal(AuthContext.ownerOnly)
                        } else null // not authenticated
                    }
                }
                bearer(AuthContext.ownerAndContacts) {
                    realm = "Briar Mailbox"
                    authenticationFunction = { credentials ->
                        LOG.error("credentials: $credentials")
                        val folderId = credentials.folderId
                        // we must have a folderId for this AuthContext
                        if (folderId == null) {
                            LOG.warn("No folderId found in request")
                            null
                        } else if (authManager.canOwnerOrContactAccess(credentials)) {
                            UserIdPrincipal(AuthContext.ownerAndContacts)
                        } else null // not authenticated
                    }
                }
            }
            configureBasicApi()
            configureContactApi()
            configureFilesApi()
        }
    }

    override fun startService() {
        server.start()
    }

    override fun stopService() {
        server.stop(1_000, 2_000)
    }

}
