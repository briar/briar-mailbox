package org.briarproject.mailbox.core.server

import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.features.CallLogging
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.briarproject.mailbox.core.lifecycle.Service
import org.slf4j.LoggerFactory.getLogger
import javax.inject.Inject
import javax.inject.Singleton

interface WebServerManager : Service

@Singleton
internal class WebServerManagerImpl @Inject constructor(
    private val authManager: AuthenticationManager,
) : WebServerManager {

    internal companion object {
        internal const val PORT = 8000
        private val LOG = getLogger(WebServerManager::class.java)
    }

    private val server by lazy {
        embeddedServer(Netty, PORT, watchPaths = emptyList()) {
            install(CallLogging)
            // TODO validate mailboxId and fileId somewhere
            install(Authentication) {
                bearer(AuthContext.ownerOnly) {
                    realm = "Briar Mailbox Owner"
                    validate { credentials ->
                        LOG.error("credentials: $credentials")
                        if (authManager.canOwnerAccess(credentials)) {
                            UserIdPrincipal(AuthContext.ownerOnly)
                        } else null // not authenticated
                    }
                }
                bearer(AuthContext.ownerAndContacts) {
                    realm = "Briar Mailbox"
                    validate { credentials ->
                        LOG.error("credentials: $credentials")
                        val mailboxId = credentials.mailboxId
                        // we must have a mailboxId for this AuthContext
                        if (mailboxId == null) {
                            LOG.warn("No mailboxId found in request")
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
