package org.briarproject.mailbox.core.contacts

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.briarproject.mailbox.core.TestUtils
import org.briarproject.mailbox.core.TestUtils.getNewRandomContact
import org.briarproject.mailbox.core.server.IntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals

class ContactsManagerMalformedInputIntegrationTest : IntegrationTest(false) {

    @BeforeEach
    override fun beforeEach() {
        super.beforeEach()
        addOwnerToken()
    }

    /**
     * This test is the same as the one from [ContactsManagerIntegrationTest], just that it supplies
     * raw JSON as a body. Unlike all other tests in this class, this one should be able to create
     * a contact. Just making sure, it is possible to specify raw JSON properly here, while all
     * other tests supply some kind of broken JSON.
     */
    @Test
    fun `owner can add contacts`(): Unit = runBlocking {
        val c1 = getNewRandomContact(1).also { addContact(it) }
        val c2 = getNewRandomContact(2).also { addContact(it) }
        val c3 = getNewRandomContact(3)

        val response1: HttpResponse = httpClient.post("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
            contentType(ContentType.Application.Json)
            body = """{
                "contactId": ${c3.contactId},
                "token": "${c3.token}",
                "inboxId": "${c3.inboxId}",
                "outboxId": "${c3.outboxId}"
            }""".trimMargin()
        }
        assertEquals(Created, response1.status)

        val response2: HttpResponse = httpClient.get("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
        }
        TestUtils.assertJson("""{ "contacts": [ 1, 2, 3 ] }""", response2)

        val db = testComponent.getDatabase()
        db.read { txn ->
            assertEquals(c1, db.getContact(txn, 1))
            assertEquals(c2, db.getContact(txn, 2))
            assertEquals(c3, db.getContact(txn, 3))
        }
    }

    /*
     * Tests with header "Content-Type: application/json" set
     */

    @Test
    fun `empty string is rejected`(): Unit = runBlocking {
        `invalid body with json content type is rejected`("")
    }

    @Test
    fun `empty object is rejected`(): Unit = runBlocking {
        `invalid body with json content type is rejected`("{}")
    }

    @Test
    fun `invalid JSON is rejected`(): Unit = runBlocking {
        `invalid body with json content type is rejected`("foo")
    }

    @Test
    fun `invalid contactId in body is rejected`(): Unit = runBlocking {
        `invalid body with json content type is rejected`(
            """{
                "contactId": foo,
                "token": "${TestUtils.getNewRandomId()}",
                "inboxId": "${TestUtils.getNewRandomId()}",
                "outboxId": "${TestUtils.getNewRandomId()}"
            }""".trimMargin()
        )
    }

    @Test
    fun `invalid token in body is rejected`(): Unit = runBlocking {
        `invalid body with json content type is rejected`(
            """{
                "contactId": 3,
                "token": "foo",
                "inboxId": "${TestUtils.getNewRandomId()}",
                "outboxId": "${TestUtils.getNewRandomId()}"
            }""".trimMargin()
        )
    }

    @Test
    fun `invalid inboxId in body is rejected`(): Unit = runBlocking {
        `invalid body with json content type is rejected`(
            """{
                "contactId": 3,
                "token": "${TestUtils.getNewRandomId()}",
                "inboxId": "123",
                "outboxId": "${TestUtils.getNewRandomId()}"
            }""".trimMargin()
        )
    }

    @Test
    fun `invalid outboxId in body is rejected`(): Unit = runBlocking {
        `invalid body with json content type is rejected`(
            """{
                "contactId": 3,
                "token": "${TestUtils.getNewRandomId()}",
                "inboxId": "${TestUtils.getNewRandomId()}",
                "outboxId": ${"0".repeat(63) + "A"}
            }""".trimMargin()
        )
    }

    private fun `invalid body with json content type is rejected`(json: Any): Unit = runBlocking {
        val c1 = getNewRandomContact(1).also { addContact(it) }
        val c2 = getNewRandomContact(2).also { addContact(it) }

        val response1: HttpResponse = httpClient.post("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
            contentType(ContentType.Application.Json)
            body = json
        }
        assertEquals(BadRequest, response1.status)

        assertContacts(c1, c2)
    }

    /*
     * Tests without header "Content-Type: application/json" set
     */

    @Test
    fun `empty body is rejected`(): Unit = runBlocking {
        val c1 = getNewRandomContact(1).also { addContact(it) }
        val c2 = getNewRandomContact(2).also { addContact(it) }

        val response1: HttpResponse = httpClient.post("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
            // It looks like here, when the JSON feature is not installed, we are not allowed to
            // set the content type explicitly, and actually, if we do, we get an exception when
            // not passing a body along with the request here
        }
        assertEquals(BadRequest, response1.status)

        assertContacts(c1, c2)
    }

    /*
     * Tests with other content type headers
     */

    @Test
    fun `plaintext content is rejected`(): Unit = runBlocking {
        val c1 = getNewRandomContact(1).also { addContact(it) }
        val c2 = getNewRandomContact(2).also { addContact(it) }

        val response1: HttpResponse = httpClient.post("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
            // cannot do this here:
            //
            //     contentType(ContentType.Text.Plain)
            //
            // but the content type will automatically be set to "text/plain; charset=UTF-8"
            // in this case
            body = "foo"
        }
        assertEquals(BadRequest, response1.status)

        assertContacts(c1, c2)
    }

    @Test
    fun `PDF content is rejected`(): Unit = runBlocking {
        val c1 = getNewRandomContact(1).also { addContact(it) }
        val c2 = getNewRandomContact(2).also { addContact(it) }

        val response1: HttpResponse = httpClient.post("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
            contentType(ContentType.Application.Pdf)
            body = Random.nextBytes(100)
        }
        assertEquals(BadRequest, response1.status)

        assertContacts(c1, c2)
    }

    /*
     * Utility methods
     */

    private suspend fun assertContacts(c1: Contact, c2: Contact) {
        val response2: HttpResponse = httpClient.get("$baseUrl/contacts") {
            authenticateWithToken(ownerToken)
        }
        TestUtils.assertJson("""{ "contacts": [ 1, 2 ] }""", response2)

        val db = testComponent.getDatabase()
        db.read { txn ->
            assertEquals(c1, db.getContact(txn, 1))
            assertEquals(c2, db.getContact(txn, 2))
        }
    }

}
