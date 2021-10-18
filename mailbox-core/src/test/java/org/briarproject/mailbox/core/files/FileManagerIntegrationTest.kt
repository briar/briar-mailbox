package org.briarproject.mailbox.core.files

import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.briarproject.mailbox.core.TestUtils.getNewRandomId
import org.briarproject.mailbox.core.server.IntegrationTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import kotlin.random.Random
import kotlin.test.assertEquals

@TestInstance(Lifecycle.PER_CLASS)
class FileManagerIntegrationTest : IntegrationTest() {

    private val bytes = Random.nextBytes(2048)

    override fun initDb() {
        addOwnerToken()
        addContact(contact1)
        addContact(contact2)
    }

    @Test
    fun `post new file rejects wrong token`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.post("$baseUrl/files/${getNewRandomId()}") {
            authenticateWithToken(token)
            body = bytes
        }
        assertEquals(HttpStatusCode.Unauthorized.value, response.status.value)
    }

    @Test
    fun `post new file rejects unauthorized folder ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.post("$baseUrl/files/${contact1.outboxId}") {
            authenticateWithToken(ownerToken)
            body = bytes
        }
        assertEquals(HttpStatusCode.Unauthorized.value, response.status.value)
    }

    @Test
    fun `post new file rejects invalid folder ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.post("$baseUrl/files/foo") {
            authenticateWithToken(ownerToken)
            body = bytes
        }
        assertEquals(HttpStatusCode.BadRequest.value, response.status.value)
        assertEquals("Malformed ID: foo", response.readText())
    }

    @Test
    fun `post new file creates new file`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.post("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(ownerToken)
            body = bytes
        }
        assertEquals(HttpStatusCode.OK.value, response.status.value)

        // TODO fetch the file later to see that it was uploaded correctly
    }

}
