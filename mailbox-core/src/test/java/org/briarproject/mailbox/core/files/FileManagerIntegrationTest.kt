package org.briarproject.mailbox.core.files

import io.ktor.client.call.receive
import io.ktor.client.request.get
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
    fun `post new file, list, download and delete it`(): Unit = runBlocking {
        // owner uploads the file
        val response: HttpResponse = httpClient.post("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(ownerToken)
            body = bytes
        }
        assertEquals(HttpStatusCode.OK.value, response.status.value)

        // contact can list the file
        val listResponse: HttpResponse = httpClient.get("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(contact1.token)
        }
        assertEquals(HttpStatusCode.OK.value, listResponse.status.value)
        val fileList: FileListResponse = listResponse.receive()
        assertEquals(1, fileList.files.size)

        // TODO fetch the file later to see that it was uploaded correctly
        val fileId = fileList.files[0].name

        // TODO delete the file to clean up again
    }

    @Test
    fun `list files rejects wrong token`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/files/${getNewRandomId()}") {
            authenticateWithToken(token)
            body = bytes
        }
        assertEquals(HttpStatusCode.Unauthorized.value, response.status.value)
    }

    @Test
    fun `list files rejects unauthorized folder ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(ownerToken)
            body = bytes
        }
        assertEquals(HttpStatusCode.Unauthorized.value, response.status.value)
    }

    @Test
    fun `list files rejects invalid folder ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/files/foo") {
            authenticateWithToken(ownerToken)
            body = bytes
        }
        assertEquals(HttpStatusCode.BadRequest.value, response.status.value)
        assertEquals("Malformed ID: foo", response.readText())
    }

    @Test
    fun `list files gives empty response for empty folder`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/files/${contact1.outboxId}") {
            authenticateWithToken(ownerToken)
            body = bytes
        }
        assertEquals(HttpStatusCode.OK.value, response.status.value)
        assertEquals("""{"files":[]}""", response.readText())
    }

}
