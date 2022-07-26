package org.briarproject.mailbox.core.settings

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.briarproject.mailbox.core.TestUtils.assertJson
import org.briarproject.mailbox.core.server.IntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetadataRouteManagerTest : IntegrationTest() {

    @BeforeEach
    override fun beforeEach() {
        super.beforeEach()
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
        assertEquals("", response.bodyAsText())
    }

    @Test
    fun `contact can access status`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/status") {
            authenticateWithToken(contact1.token)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("", response.bodyAsText())
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

    @Test
    fun `owner can request versions and response contains supported versions`() = runBlocking {
        addOwnerToken()

        val response: VersionsResponse = httpClient.get("$baseUrl/versions") {
            authenticateWithToken(ownerToken)
        }.body()

        assertTrue(response.serverSupports.contains(MailboxVersion(1, 0)))
    }

    @Test
    fun `owner can request versions and response contains supported versions in correct format`() =
        runBlocking {
            addOwnerToken()

            val response: HttpResponse = httpClient.get("$baseUrl/versions") {
                authenticateWithToken(ownerToken)
            }

            // make sure the response is in the expected format, allowing whitespace variations
            assertJson("""{ "serverSupports": [ {"major": 1, "minor": 0} ] }""", response)
        }

    @Test
    fun `contact cannot request versions`(): Unit = runBlocking {
        addOwnerToken()
        val response: HttpResponse = httpClient.get("$baseUrl/versions") {
            authenticateWithToken(contact1.token)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `wrong token cannot request versions`(): Unit = runBlocking {
        addOwnerToken()
        val response: HttpResponse = httpClient.get("$baseUrl/versions") {
            authenticateWithToken(token)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `empty token cannot request versions`(): Unit = runBlocking {
        addOwnerToken()
        val response: HttpResponse = httpClient.get("$baseUrl/versions") {
            authenticateWithToken("")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

}
