package org.briarproject.mailbox.core.setup

import io.ktor.client.request.delete
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.briarproject.mailbox.core.server.IntegrationTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WipeRouteManagerTest : IntegrationTest() {

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

}
