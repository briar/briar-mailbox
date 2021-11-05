package org.briarproject.mailbox.core.server

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.features.BadRequestException
import io.ktor.features.MissingRequestParameterException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.getOrFail
import org.briarproject.mailbox.core.contacts.ContactsManager
import org.briarproject.mailbox.core.files.FileManager
import org.briarproject.mailbox.core.setup.SetupManager
import org.briarproject.mailbox.core.setup.WipeManager
import org.briarproject.mailbox.core.system.InvalidIdException

internal const val V = "/" // TODO set to "/v1" for release

internal fun Application.configureBasicApi(
    setupManager: SetupManager,
    wipeManager: WipeManager,
) = routing {
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
                    if (call.principal<MailboxPrincipal>() !is MailboxPrincipal.OwnerPrincipal)
                        throw AuthException()
                    call.respond(HttpStatusCode.OK)
                }
            }
            delete {
                call.handle {
                    wipeManager.onWipeRequest(call)
                }
            }
            put("/setup") {
                call.handle {
                    setupManager.onSetupRequest(call)
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

internal fun Application.configureFilesApi(fileManager: FileManager) = routing {

    authenticate {
        route("$V/files/{folderId}") {
            post {
                call.handle {
                    fileManager.postFile(call, call.parameters.getOrFail("folderId"))
                }
            }
            get {
                call.handle {
                    fileManager.listFiles(call, call.parameters.getOrFail("folderId"))
                }
            }
            route("/{fileId}") {
                get {
                    val folderId = call.parameters.getOrFail("folderId")
                    val fileId = call.parameters.getOrFail("fileId")
                    call.handle {
                        fileManager.getFile(call, folderId, fileId)
                    }
                }
                delete {
                    val folderId = call.parameters.getOrFail("folderId")
                    val fileId = call.parameters.getOrFail("fileId")
                    call.handle {
                        fileManager.deleteFile(call, folderId, fileId)
                    }
                }
            }
        }
    }
    authenticate {
        get("$V/folders") {
            call.handle {
                fileManager.listFoldersWithFiles(call)
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
