package org.briarproject.mailbox.core.server

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing

internal fun Application.configureRouting() = routing {
    get("/") {
        call.respondText("Hello world!", ContentType.Text.Plain)
    }
}
