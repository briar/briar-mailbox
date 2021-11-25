package org.briarproject.mailbox.core.contacts

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.briarproject.mailbox.core.TestUtils.assertJson
import org.briarproject.mailbox.core.TestUtils.assertTimestampRecent
import org.briarproject.mailbox.core.TestUtils.getNewRandomContact
import org.briarproject.mailbox.core.server.IntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ContactsManagerIntegrationTest : IntegrationTest() {

    @BeforeEach
    fun initDb() {
        addOwnerToken()
    }

    @AfterEach
    fun clearDb() {
        db.write { txn ->
            db.clearDatabase(txn)
            // clears [metadataManager.ownerConnectionTime]
            metadataManager.onDatabaseOpened(txn)
        }
    }

    @Test
    fun `get contacts is initially empty`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        val response: HttpResponse = httpClient.get("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
        }
        assertJson("""{ "contacts": [ ] }""", response)

        assertTimestampRecent(metadataManager.ownerConnectionTime.value)
    }

    @Test
    fun `get contacts returns correct ids`(): Unit = runBlocking {
        addContact(contact1)
        addContact(contact2)
        val response: HttpResponse = httpClient.get("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
        }
        assertJson("""{ "contacts": [ ${contact1.contactId}, ${contact2.contactId} ] }""", response)
    }

    @Test
    fun `get contacts rejects unauthorized for contacts`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        addContact(contact1)
        addContact(contact2)
        val response: HttpResponse = httpClient.get("$baseUrl/contacts") {
            authenticateWithToken(contact1.token)
        }
        assertEquals(Unauthorized, response.status)
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
    }

    @Test
    fun `get contacts rejects unauthorized without token`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/contacts")
        assertEquals(Unauthorized, response.status)
    }

    @Test
    fun `get contacts rejects unauthorized for random token`(): Unit = runBlocking {
        addContact(contact1)
        addContact(contact2)
        val response: HttpResponse = httpClient.get("$baseUrl/contacts") {
            authenticateWithToken(token)
        }
        assertEquals(Unauthorized, response.status)
    }

    @Test
    fun `get contacts rejects unauthorized for invalid token (too short)`(): Unit = runBlocking {
        addContact(contact1)
        addContact(contact2)
        val response: HttpResponse = httpClient.get("$baseUrl/contacts") {
            authenticateWithToken("abc0123")
        }
        assertEquals(Unauthorized, response.status)
    }

    @Test
    fun `get contacts rejects unauthorized for invalid token (illegal characters)`(): Unit =
        runBlocking {
            addContact(contact1)
            addContact(contact2)
            val response: HttpResponse = httpClient.get("$baseUrl/contacts") {
                // letters outside a-f and dot not allowed, only [a-f0-9]{64} is valid
                authenticateWithToken("foo.bar")
            }
            assertEquals(Unauthorized, response.status)
        }

    @Test
    fun `owner can add contacts`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        val c1 = getNewRandomContact(1).also { addContact(it) }
        val c2 = getNewRandomContact(2).also { addContact(it) }
        val c3 = getNewRandomContact(3)

        val response1: HttpResponse = httpClient.post("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
            contentType(ContentType.Application.Json)
            body = c3
        }
        assertEquals(Created, response1.status)

        assertTimestampRecent(metadataManager.ownerConnectionTime.value)

        val response2: HttpResponse = httpClient.get("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
        }
        assertJson("""{ "contacts": [ 1, 2, 3 ] }""", response2)

        db.read { txn ->
            assertEquals(c1, db.getContact(txn, 1))
            assertEquals(c2, db.getContact(txn, 2))
            assertEquals(c3, db.getContact(txn, 3))
        }
    }

    @Test
    fun `contact cannot add contacts`(): Unit = runBlocking {
        addContact(contact1)
        addContact(contact2)

        val response1: HttpResponse = httpClient.post("$baseUrl/contacts") {
            authenticateWithToken(contact1.token)
            contentType(ContentType.Application.Json)
            body = getNewRandomContact(3)
        }
        assertEquals(Unauthorized, response1.status)

        val response2: HttpResponse = httpClient.get("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
        }
        assertJson(
            """{ "contacts": [ ${contact1.contactId}, ${contact2.contactId} ] }""",
            response2
        )
    }

    @Test
    fun `owner can remove contacts`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        addContact(getNewRandomContact(1))
        addContact(getNewRandomContact(2))

        val response1: HttpResponse = httpClient.delete("$baseUrl/contacts/1") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(OK, response1.status)

        assertTimestampRecent(metadataManager.ownerConnectionTime.value)

        val response2: HttpResponse = httpClient.get("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
        }
        assertJson("""{ "contacts": [ 2 ] }""", response2)
    }

    @Test
    fun `contact cannot remove contacts`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        addContact(contact1)
        addContact(contact2)

        val response1: HttpResponse = httpClient.delete("$baseUrl/contacts/1") {
            authenticateWithToken(contact2.token)
        }
        assertEquals(Unauthorized, response1.status)
        assertEquals(0L, metadataManager.ownerConnectionTime.value)

        val response2: HttpResponse = httpClient.get("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
        }
        assertJson(
            """{ "contacts": [ ${contact1.contactId}, ${contact2.contactId} ] }""",
            response2
        )
    }

    @Test
    fun `adding contact with existing contactId is rejected`(): Unit = runBlocking {
        addContact(getNewRandomContact(1))
        addContact(getNewRandomContact(2))

        val response1: HttpResponse = httpClient.post("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
            contentType(ContentType.Application.Json)
            body = getNewRandomContact(2)
        }
        assertEquals(Conflict, response1.status)

        val response2: HttpResponse = httpClient.get("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
        }
        assertJson("""{ "contacts": [ 1, 2 ] }""", response2)
    }

    @Test
    fun `removing non-existent contacts fails gracefully`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        addContact(getNewRandomContact(1))
        addContact(getNewRandomContact(2))

        val response1: HttpResponse = httpClient.delete("$baseUrl/contacts/3") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(NotFound, response1.status)

        // still registers owner connection
        assertTimestampRecent(metadataManager.ownerConnectionTime.value)

        val response2: HttpResponse = httpClient.get("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
        }
        assertJson("""{ "contacts": [ 1, 2 ] }""", response2)
    }

    /*
     * Tests about malformed input
     */

    @Test
    fun `contact removal with missing contactId is rejected`(): Unit = runBlocking {
        addContact(getNewRandomContact(1))
        addContact(getNewRandomContact(2))

        val response: HttpResponse = httpClient.delete("$baseUrl/contacts/") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(NotFound, response.status)
    }

    @Test
    fun `contact removal with non-integer contactId is rejected`(): Unit = runBlocking {
        addContact(getNewRandomContact(1))
        addContact(getNewRandomContact(2))

        val response: HttpResponse = httpClient.delete("$baseUrl/contacts/foo") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(BadRequest, response.status)
        assertEquals("Bad request: Invalid value for parameter contactId", response.readText())
    }
}
