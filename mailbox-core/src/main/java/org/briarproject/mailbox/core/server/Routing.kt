package org.briarproject.mailbox.core.server

import io.ktor.application.Application
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

internal const val V = "" // TODO set to "/v1" for release

internal fun Application.configureBasicApi() = routing {
    route("$V/") {
        get {
            call.respondText("Hello world!", ContentType.Text.Plain)
        }
        authenticate(AuthContext.ownerOnly) {
            delete {
                call.respond(HttpStatusCode.OK, "delete: Not yet implemented")
            }
            put("setup") {
                call.respond(HttpStatusCode.OK, "put: Not yet implemented")
            }
        }
    }
}

internal fun Application.configureContactApi() = routing {
    authenticate(AuthContext.ownerOnly) {
        route("$V/contacts") {
            put("{contactId}") {
                call.respond(
                    HttpStatusCode.OK,
                    "get: Not yet implemented. " +
                        "contactId: ${call.parameters["contactId"]}"
                )
            }
            delete("{contactId}") {
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

internal fun Application.configureFilesApi() = routing {

    authenticate(AuthContext.ownerAndContacts) {
        route("$V/files/{folderId}") {
            post {
                call.respond(
                    HttpStatusCode.OK,
                    "post: Not yet implemented. " +
                        "folderId: ${call.parameters["folderId"]}"
                )
            }
            get {
                call.respond(
                    HttpStatusCode.OK,
                    "get: Not yet implemented. " +
                        "folderId: ${call.parameters["folderId"]}"
                )
            }
            route("/{fileId}") {
                get {
                    call.respond(
                        HttpStatusCode.OK,
                        "get: Not yet implemented. " +
                            "folderId: ${call.parameters["folderId"]} " +
                            "fileId: ${call.parameters["fileId"]}"
                    )
                }
                delete {
                    call.respond(
                        HttpStatusCode.OK,
                        "delete: Not yet implemented. " +
                            "folderId: ${call.parameters["folderId"]} " +
                            "fileId: ${call.parameters["fileId"]}"
                    )
                }
            }
        }
    }
    authenticate(AuthContext.ownerOnly) {
        get("$V/mailboxes") {
            call.respond(HttpStatusCode.OK, "get: Not yet implemented")
        }
    }
}
