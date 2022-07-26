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

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import org.briarproject.mailbox.core.contacts.ContactsManager
import org.briarproject.mailbox.core.files.FileRouteManager
import org.briarproject.mailbox.core.settings.MetadataRouteManager
import org.briarproject.mailbox.core.setup.SetupRouteManager
import org.briarproject.mailbox.core.setup.WipeRouteManager
import org.briarproject.mailbox.core.system.InvalidIdException

internal const val V = "/" // TODO set to "/v1" for release

internal fun Application.configureBasicApi(
    metadataRouteManager: MetadataRouteManager,
    setupRouteManager: SetupRouteManager,
    wipeRouteManager: WipeRouteManager,
) = routing {
    authenticate {
        get("/versions") {
            call.handle {
                metadataRouteManager.onVersionsRequest(call)
            }
        }
    }
    route(V) {
        get {
            call.respondText(
                "Hello, I'm a Briar teapot",
                ContentType.Text.Plain,
                HttpStatusCode(418, "I'm a teapot")
            )
        }
        authenticate {
            get("/status") {
                call.handle {
                    metadataRouteManager.onStatusRequest(call)
                }
            }
            delete {
                call.handle {
                    wipeRouteManager.onWipeRequest(call)
                }
            }
            put("/setup") {
                call.handle {
                    setupRouteManager.onSetupRequest(call)
                }
            }
        }
    }
}

internal fun Application.configureContactApi(contactsManager: ContactsManager) =
    routing {
        authenticate {
            route("$V/contacts") {
                post {
                    call.handle {
                        contactsManager.postContact(call)
                    }
                }
                delete("/{contactId}") {
                    call.handle {
                        val contactId = call.parameters.getOrFail("contactId")
                        contactsManager.deleteContact(call, contactId)
                    }
                }
                get {
                    call.handle {
                        contactsManager.listContacts(call)
                    }
                }
            }
        }
    }

internal fun Application.configureFilesApi(fileRouteManager: FileRouteManager) = routing {

    authenticate {
        route("$V/files/{folderId}") {
            post {
                call.handle {
                    fileRouteManager.postFile(call, call.parameters.getOrFail("folderId"))
                }
            }
            get {
                call.handle {
                    fileRouteManager.listFiles(call, call.parameters.getOrFail("folderId"))
                }
            }
            route("/{fileId}") {
                get {
                    val folderId = call.parameters.getOrFail("folderId")
                    val fileId = call.parameters.getOrFail("fileId")
                    call.handle {
                        fileRouteManager.getFile(call, folderId, fileId)
                    }
                }
                delete {
                    val folderId = call.parameters.getOrFail("folderId")
                    val fileId = call.parameters.getOrFail("fileId")
                    call.handle {
                        fileRouteManager.deleteFile(call, folderId, fileId)
                    }
                }
            }
        }
    }
    authenticate {
        get("$V/folders") {
            call.handle {
                fileRouteManager.listFoldersWithFiles(call)
            }
        }
    }
}

private suspend fun ApplicationCall.handle(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: AuthException) {
        respond(Unauthorized, Unauthorized.description)
    } catch (e: InvalidIdException) {
        respond(BadRequest, "Malformed ID: ${e.id}")
    } catch (e: MissingRequestParameterException) {
        respond(BadRequest, "Missing parameter: ${e.parameterName}")
    } catch (e: BadRequestException) {
        respond(BadRequest, "Bad request: ${e.message}")
    }
}
