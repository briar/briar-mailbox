package org.briarproject.mailbox.core.server

import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StatusIntegrationTest : IntegrationTest() {

    @BeforeEach
    fun initDb() {
        addOwnerToken()
        addContact(contact1)
        addContact(contact2)
    }

    @Test
    fun `owner can access status`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/status") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("", response.readText())
    }

    @Test
    fun `contact cannot access status`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/status") {
            authenticateWithToken(contact1.token)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `wrong token cannot access status`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/status") {
            authenticateWithToken(token)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `empty token cannot access status`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/status") {
            authenticateWithToken("")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

}
