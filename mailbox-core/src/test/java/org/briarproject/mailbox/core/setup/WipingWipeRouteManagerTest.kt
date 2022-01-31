package org.briarproject.mailbox.core.setup

import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.briarproject.mailbox.core.db.DbException
import org.briarproject.mailbox.core.server.IntegrationTest
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WipingWipeRouteManagerTest : IntegrationTest() {

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

        // assert that database is gone
        assertFalse(testComponent.getDatabaseConfig().getDatabaseDirectory().exists())

        // no more files are stored
        val folderRoot = testComponent.getFileProvider().folderRoot
        assertFalse(folderRoot.exists())

        // file root has been cleared
        val root = testComponent.getFileProvider().root
        assertTrue(root.listFiles()?.isEmpty() ?: false)

        // no more contacts in DB - contacts table is gone
        // it actually fails because db is closed though
        assertFailsWith<DbException> { db.read { db.getContacts(it) } }

        // owner token was cleared as well - settings table is gone
        // it actually fails because db is closed though
        assertFailsWith<DbException> {
            db.read { txn ->
                testComponent.getSetupManager().getOwnerToken(txn)
            }
        }

        // re-open the database
        db.open(null)

        // reopening re-created the database directory
        assertTrue(testComponent.getDatabaseConfig().getDatabaseDirectory().exists())

        // no more contacts in DB
        assertTrue(db.read { db.getContacts(it) }.isEmpty())

        // owner token was cleared as well
        assertNull(
            db.read { txn ->
                testComponent.getSetupManager().getOwnerToken(txn)
            }
        )
    }

}
