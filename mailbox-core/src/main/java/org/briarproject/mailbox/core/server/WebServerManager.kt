/*
 *     Briar Mailbox
 *     Copyright (C) 2021-2022  The Briar Project
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.briarproject.mailbox.core.server

import com.fasterxml.jackson.databind.MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import org.briarproject.mailbox.core.contacts.ContactsManager
import org.briarproject.mailbox.core.files.FileRouteManager
import org.briarproject.mailbox.core.lifecycle.Service
import org.briarproject.mailbox.core.server.WebServerManager.Companion.PORT
import org.briarproject.mailbox.core.settings.MetadataRouteManager
import org.briarproject.mailbox.core.setup.SetupRouteManager
import org.briarproject.mailbox.core.setup.WipeRouteManager
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
    private val fileRouteManager: FileRouteManager,
    private val wipeRouteManager: WipeRouteManager,
) : WebServerManager {

    private val server by lazy {
        embeddedServer(Netty, PORT, watchPaths = emptyList()) {
            install(CallLogging)
            install(Authentication) {
                bearer {
                    authenticationFunction = { token ->
                        authManager.getPrincipal(token)
                    }
                }
            }
            install(ContentNegotiation) {
                jackson {
                    enable(BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                }
            }
            configureBasicApi(metadataRouteManager, setupRouteManager, wipeRouteManager)
            configureContactApi(contactsManager)
            configureFilesApi(fileRouteManager)
        }
    }

    override fun startService() {
        server.start()
    }

    override fun stopService() {
        server.stop(1_000, 2_000)
    }

}
