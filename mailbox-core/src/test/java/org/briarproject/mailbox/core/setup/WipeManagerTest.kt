package org.briarproject.mailbox.core.setup

import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.briarproject.mailbox.core.server.IntegrationTest
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WipeManagerTest : IntegrationTest() {

    private val db by lazy { testComponent.getDatabase() }

    @Test
    fun `wipe request rejects non-owners`() = runBlocking {
        addOwnerToken()
        addContact(contact1)

        // Unauthorized with random token
        val response1 = httpClient.delete<HttpResponse>("$baseUrl/") {
            authenticateWithToken(token)
        }
        assertEquals(HttpStatusCode.Unauthorized, response1.status)

        // Unauthorized with contact's token
        val response2 = httpClient.delete<HttpResponse>("$baseUrl/") {
            authenticateWithToken(contact1.token)
        }
        assertEquals(HttpStatusCode.Unauthorized, response2.status)
    }

    @Test
    fun `wipe request deletes files and db for owner`() = runBlocking {
        addOwnerToken()
        addContact(contact1)
        addContact(contact2)

        // owner uploads a file
        val uploadResponse: HttpResponse = httpClient.post("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(ownerToken)
            body = Random.nextBytes(42)
        }
        assertEquals(HttpStatusCode.OK, uploadResponse.status)

        // owner wipes mailbox
        val response = httpClient.delete<HttpResponse>("$baseUrl/") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.NoContent, response.status)

        // no more contacts in DB
        val contacts = db.read { db.getContacts(it) }
        assertEquals(0, contacts.size)

        // owner token was cleared as well
        val token = db.read { txn ->
            testComponent.getSetupManager().getOwnerToken(txn)
        }
        assertNull(token)

        // no more files are stored
        val folderRoot = testComponent.getFileProvider().folderRoot
        assertTrue(folderRoot.listFiles()?.isEmpty() ?: false)
    }

}
