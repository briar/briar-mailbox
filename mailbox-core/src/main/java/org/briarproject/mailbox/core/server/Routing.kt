package org.briarproject.mailbox.core.server

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.getOrFail
import org.briarproject.mailbox.core.files.FileManager
import org.briarproject.mailbox.core.system.InvalidIdException

internal const val V = "/" // TODO set to "/v1" for release

internal fun Application.configureBasicApi() = routing {
    route(V) {
        get {
            call.respondText("Hello world!", ContentType.Text.Plain)
        }
        authenticate {
            delete {
                call.respond(HttpStatusCode.OK, "delete: Not yet implemented")
            }
            put("/setup") {
                call.respond(HttpStatusCode.OK, "put: Not yet implemented")
            }
        }
    }
}

internal fun Application.configureContactApi() = routing {
    authenticate {
        route("$V/contacts") {
            put("/{contactId}") {
                call.respond(
                    HttpStatusCode.OK,
                    "get: Not yet implemented. " +
                        "contactId: ${call.parameters["contactId"]}"
                )
            }
            delete("/{contactId}") {
                call.respond(
                    HttpStatusCode.OK,
                    "delete: Not yet implemented. " +
                        "contactId: ${call.parameters["contactId"]}"
                )
            }
            get {
                call.respond(HttpStatusCode.OK, "get: Not yet implemented")
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
    } catch (e: AuthenticationException) {
        respond(HttpStatusCode.Unauthorized, HttpStatusCode.Unauthorized.description)
    } catch (e: InvalidIdException) {
        respond(HttpStatusCode.BadRequest, "Malformed ID: ${e.id}")
    }
}
