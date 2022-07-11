package org.briarproject.mailbox.core.server

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.MapperFeature.BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import org.briarproject.mailbox.core.server.WebServerManager.Companion.PORT
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WebServerIntegrationTest : IntegrationTest() {

    @Test
    fun routeRespondsWithTeapot(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/")
        assertEquals(418, response.status.value)
        assertEquals("Hello, I'm a Briar teapot", response.bodyAsText())
    }

    @Test
    fun routeNotFound(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/404")
        assertEquals(404, response.status.value)
    }

    @Test
    fun testJacksonUnsafeDeserialization(): Unit = runBlocking {
        val port = PORT + 1
        val server = embeddedServer(Netty, port, watchPaths = emptyList()) {
            install(CallLogging)
            install(ContentNegotiation) {
                jackson {
                    enable(BLOCK_UNSAFE_POLYMORPHIC_BASE_TYPES)
                }
            }
            routing {
                post("/") {
                    println(call.receive<Wrapper>())
                    call.respond(HttpStatusCode.OK, "OK")
                }
            }
        }
        try {
            server.start()
            val response: HttpResponse = httpClient.post("http://127.0.0.1:$port/") {
                contentType(ContentType.Application.Json)
                setBody(Wrapper().apply { value = "foo" })
            }
            assertEquals(400, response.status.value)
        } finally {
            server.stop(0, 0)
        }
    }

    internal class Wrapper {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        var value: Any? = null
    }

}
