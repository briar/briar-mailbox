package org.briarproject.mailbox.core.files

import io.ktor.client.call.receive
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.briarproject.mailbox.core.TestUtils.assertTimestampRecent
import org.briarproject.mailbox.core.TestUtils.getNewRandomId
import org.briarproject.mailbox.core.server.IntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals

class FileManagerIntegrationTest : IntegrationTest() {

    private val bytes = Random.nextBytes(2048)

    @BeforeEach
    override fun initDb() {
        super.initDb()
        addOwnerToken()
        addContact(contact1)
        addContact(contact2)
    }

    @AfterEach
    override fun clearDb() {
        super.clearDb()
        testComponent.getFileManager().deleteAllFiles()
    }

    @Test
    fun `post new file rejects wrong token`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.post("$baseUrl/files/${getNewRandomId()}") {
            authenticateWithToken(token)
            body = bytes
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `post new file rejects unauthorized folder ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.post("$baseUrl/files/${contact1.outboxId}") {
            authenticateWithToken(ownerToken)
            body = bytes
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `post new file rejects invalid folder ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.post("$baseUrl/files/foo") {
            authenticateWithToken(ownerToken)
            body = bytes
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Malformed ID: foo", response.readText())
    }

    @Test
    fun `post new file, list, download and delete it`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        // owner uploads the file
        val response: HttpResponse = httpClient.post("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(ownerToken)
            body = bytes
        }
        assertEquals(HttpStatusCode.OK, response.status)
        // owner connection got registered
        assertTimestampRecent(metadataManager.ownerConnectionTime.value)

        // contact can list the file
        val listResponse: HttpResponse = httpClient.get("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(contact1.token)
        }
        assertEquals(HttpStatusCode.OK, listResponse.status)
        val fileList: FileListResponse = listResponse.receive()
        assertEquals(1, fileList.files.size)

        // contact can download the file
        val fileId = fileList.files[0].name
        val fileResponse: HttpResponse =
            httpClient.get("$baseUrl/files/${contact1.inboxId}/$fileId") {
                authenticateWithToken(contact1.token)
            }
        assertEquals(HttpStatusCode.OK, fileResponse.status)
        assertArrayEquals(bytes, fileResponse.readBytes())

        // contact can delete the file
        val deleteResponse: HttpResponse =
            httpClient.delete("$baseUrl/files/${contact1.inboxId}/$fileId") {
                authenticateWithToken(contact1.token)
            }
        assertEquals(HttpStatusCode.OK, deleteResponse.status)

        // now the list of files in that folder is empty again
        val emptyFileListResponse: FileListResponse =
            httpClient.get("$baseUrl/files/${contact1.inboxId}") {
                authenticateWithToken(contact1.token)
            }
        assertEquals(0, emptyFileListResponse.files.size)
    }

    @Test
    fun `list files rejects wrong token`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        val response: HttpResponse = httpClient.get("$baseUrl/files/${getNewRandomId()}") {
            authenticateWithToken(token)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(0L, metadataManager.ownerConnectionTime.value)

        // upload a real file
        val postResponse: HttpResponse = httpClient.post("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(ownerToken)
            body = bytes
        }
        assertEquals(HttpStatusCode.OK, postResponse.status)

        // wrong token also gets rejected for real folder
        val lastResponse: HttpResponse = httpClient.get("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(token)
        }
        assertEquals(HttpStatusCode.Unauthorized, lastResponse.status)
    }

    @Test
    fun `list files rejects unauthorized folder ID`(): Unit = runBlocking {
        val response1: HttpResponse = httpClient.get("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.Unauthorized, response1.status)

        val response2: HttpResponse = httpClient.get("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(contact2.token)
        }
        assertEquals(HttpStatusCode.Unauthorized, response2.status)
    }

    @Test
    fun `list files rejects invalid folder ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/files/foo") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Malformed ID: foo", response.readText())
    }

    @Test
    fun `list files gives empty response for empty folder`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        val response: HttpResponse = httpClient.get("$baseUrl/files/${contact1.outboxId}") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"files":[]}""", response.readText())
        // owner connection got registered
        assertTimestampRecent(metadataManager.ownerConnectionTime.value)
    }

    @Test
    fun `get file rejects wrong token`(): Unit = runBlocking {
        val response: HttpResponse =
            httpClient.get("$baseUrl/files/${getNewRandomId()}/${getNewRandomId()}") {
                authenticateWithToken(token)
            }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `get file rejects unauthorized folder ID`(): Unit = runBlocking {
        val response: HttpResponse =
            httpClient.get("$baseUrl/files/${contact1.inboxId}/${getNewRandomId()}") {
                authenticateWithToken(ownerToken)
            }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `get file rejects invalid folder ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/files/foo/${getNewRandomId()}") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Malformed ID: foo", response.readText())
    }

    @Test
    fun `get file rejects invalid file ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/files/${contact1.outboxId}/bar") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Malformed ID: bar", response.readText())
    }

    @Test
    fun `get file gives 404 response for unknown file`(): Unit = runBlocking {
        val id = getNewRandomId()
        val response: HttpResponse = httpClient.get("$baseUrl/files/${contact1.outboxId}/$id") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `get file for contact does not update owner timestamp`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        val id = getNewRandomId()
        val response: HttpResponse = httpClient.get("$baseUrl/files/${contact1.inboxId}/$id") {
            authenticateWithToken(contact1.token)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
    }

    @Test
    fun `delete file rejects wrong token`(): Unit = runBlocking {
        val response: HttpResponse =
            httpClient.delete("$baseUrl/files/${getNewRandomId()}/${getNewRandomId()}") {
                authenticateWithToken(token)
            }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `delete file rejects unauthorized folder ID`(): Unit = runBlocking {
        val response: HttpResponse =
            httpClient.delete("$baseUrl/files/${contact1.inboxId}/${getNewRandomId()}") {
                authenticateWithToken(ownerToken)
            }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `delete file rejects invalid folder ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.delete("$baseUrl/files/foo/${getNewRandomId()}") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Malformed ID: foo", response.readText())
    }

    @Test
    fun `delete file rejects invalid file ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.delete("$baseUrl/files/${contact1.outboxId}/bar") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Malformed ID: bar", response.readText())
    }

    @Test
    fun `delete file gives 404 response for unknown file`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        val id = getNewRandomId()
        val response: HttpResponse = httpClient.delete("$baseUrl/files/${contact1.outboxId}/$id") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
        // owner connection still got registered
        assertTimestampRecent(metadataManager.ownerConnectionTime.value)
    }

    @Test
    fun `delete file for contact does not update owner timestamp`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        val id = getNewRandomId()
        val response: HttpResponse = httpClient.delete("$baseUrl/files/${contact1.inboxId}/$id") {
            authenticateWithToken(contact1.token)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
    }

    @Test
    fun `list folders rejects contacts`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        val response: HttpResponse = httpClient.get("$baseUrl/folders") {
            authenticateWithToken(contact1.token)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
    }

    @Test
    fun `list folders allows owner, returns empty result`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        val response: HttpResponse = httpClient.get("$baseUrl/folders") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"folders":[]}""", response.readText())
        // owner connection got registered
        assertTimestampRecent(metadataManager.ownerConnectionTime.value)
    }

    @Test
    fun `list folders returns more than a single folder`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        // contact1 uploads a file
        val response1: HttpResponse = httpClient.post("$baseUrl/files/${contact1.outboxId}") {
            authenticateWithToken(contact1.token)
            body = bytes
        }
        assertEquals(HttpStatusCode.OK, response1.status)

        // contact2 uploads a file
        val response2: HttpResponse = httpClient.post("$baseUrl/files/${contact2.outboxId}") {
            authenticateWithToken(contact2.token)
            body = bytes
        }
        assertEquals(HttpStatusCode.OK, response2.status)
        assertEquals(0L, metadataManager.ownerConnectionTime.value)

        // owner now sees both contacts' outboxes in folders list
        val folderListResponse: FolderListResponse = httpClient.get("$baseUrl/folders") {
            authenticateWithToken(ownerToken)
        }
        val folderList = setOf(FolderResponse(contact1.outboxId), FolderResponse(contact2.outboxId))
        assertEquals(folderList, folderListResponse.folders.toSet())
        // owner connection got registered
        assertTimestampRecent(metadataManager.ownerConnectionTime.value)
    }
}
