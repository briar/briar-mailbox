package org.briarproject.mailbox.core.server

import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WebServerIntegrationTest : IntegrationTest() {

    @Test
    fun routeRespondsWithHelloWorldString(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/")
        assertEquals(200, response.status.value)
        assertEquals("Hello world!", response.readText())
    }

    @Test
    fun routeNotFound(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/404")
        assertEquals(404, response.status.value)
    }

}
