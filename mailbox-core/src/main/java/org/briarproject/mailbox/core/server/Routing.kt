package org.briarproject.mailbox.core.server

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.routing.routing

internal const val V = "/" // TODO set to "/v1" for release

internal fun Application.configureBasicApi() = routing {

    route("$V/") {
        get {
            call.respondText("Hello world!", ContentType.Text.Plain)
        }
        delete {
            TODO("Not yet implemented")
        }
    }

    put("$V/setup") {
        TODO("Not yet implemented")
    }

}

internal fun Application.configureContactApi() = routing {

    route("$V/contacts") {
        put("{contactId}") {
            TODO("Not yet implemented. contactId: ${call.parameters["contactId"]}")
        }
        delete("{contactId}") {
            TODO("Not yet implemented. contactId: ${call.parameters["contactId"]}")
        }
        get {
            TODO("Not yet implemented")
        }
    }

}

internal fun Application.configureFilesApi() = routing {

    route("$V/files/{mailboxId}") {
        post {
            TODO("Not yet implemented. mailboxId: ${call.parameters["mailboxId"]}")
        }
        get {
            TODO("Not yet implemented. mailboxId: ${call.parameters["mailboxId"]}")
        }
        route("/{fileId}") {
            get {
                TODO(
                    "Not yet implemented. mailboxId: ${call.parameters["mailboxId"]}" +
                        "fileId: ${call.parameters["fileId"]}"
                )
            }
            delete {
                TODO(
                    "Not yet implemented. mailboxId: ${call.parameters["mailboxId"]}" +
                        "fileId: ${call.parameters["fileId"]}"
                )
            }
        }
    }

    get("$V/mailboxes") {
        TODO("Not yet implemented")
    }

}
