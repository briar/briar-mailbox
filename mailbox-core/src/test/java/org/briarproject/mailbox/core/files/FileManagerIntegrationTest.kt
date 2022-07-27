package org.briarproject.mailbox.core.files

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.briarproject.mailbox.core.TestUtils.assertTimestampRecent
import org.briarproject.mailbox.core.TestUtils.getNewRandomId
import org.briarproject.mailbox.core.server.IntegrationTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.TimeUnit.DAYS
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileManagerIntegrationTest : IntegrationTest() {

    private val fileProvider by lazy { testComponent.getFileProvider() }
    private val bytes = Random.nextBytes(2048)

    @BeforeEach
    override fun beforeEach() {
        super.beforeEach()
        addOwnerToken()
        addContact(contact1)
        addContact(contact2)
    }

    @Test
    fun `post new file rejects wrong token`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.post("$baseUrl/files/${getNewRandomId()}") {
            authenticateWithToken(token)
            setBody(bytes)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `post new file rejects unauthorized folder ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.post("$baseUrl/files/${contact1.outboxId}") {
            authenticateWithToken(ownerToken)
            setBody(bytes)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `post new file rejects invalid folder ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.post("$baseUrl/files/foo") {
            authenticateWithToken(ownerToken)
            setBody(bytes)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("\"Malformed ID: foo\"", response.bodyAsText())
    }

    @Test
    fun `post new file rejects large file`(): Unit = runBlocking {
        val maxBytes = Random.nextBytes(MAX_FILE_SIZE + 1)
        // owner uploads a file above limit
        val response: HttpResponse = httpClient.post("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(ownerToken)
            setBody(maxBytes)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("\"Bad request: File larger than allowed.\"", response.bodyAsText())
        assertNoTmpFiles()
    }

    @Test
    fun `post new file with max size gets accepted`(): Unit = runBlocking {
        val maxBytes = Random.nextBytes(MAX_FILE_SIZE)
        // owner uploads the file
        val response: HttpResponse = httpClient.post("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(ownerToken)
            setBody(maxBytes)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertNoTmpFiles()
        assertNumFiles(1)
    }

    @Test
    fun `post new file, list, download and delete it`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        // owner uploads the file
        val response: HttpResponse = httpClient.post("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(ownerToken)
            setBody(bytes)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertNoTmpFiles()
        assertNumFiles(1)
        // owner connection got registered
        assertTimestampRecent(metadataManager.ownerConnectionTime.value)

        // contact can list the file
        val listResponse: HttpResponse = httpClient.get("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(contact1.token)
        }
        assertEquals(HttpStatusCode.OK, listResponse.status)
        val fileList: FileListResponse = listResponse.body()
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
            }.body()
        assertEquals(0, emptyFileListResponse.files.size)
        assertNoTmpFiles()
        assertNumFiles(0)
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
            setBody(bytes)
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
        assertEquals(HttpStatusCode.NotFound, response1.status)

        val response2: HttpResponse = httpClient.get("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(contact2.token)
        }
        assertEquals(HttpStatusCode.NotFound, response2.status)
    }

    @Test
    fun `list files rejects invalid folder ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/files/foo") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("\"Malformed ID: foo\"", response.bodyAsText())
    }

    @Test
    fun `list files gives empty response for empty folder`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        val response: HttpResponse = httpClient.get("$baseUrl/files/${contact1.outboxId}") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"files":[]}""", response.bodyAsText())
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
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `get file rejects invalid folder ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/files/foo/${getNewRandomId()}") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("\"Malformed ID: foo\"", response.bodyAsText())
    }

    @Test
    fun `get file rejects invalid file ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.get("$baseUrl/files/${contact1.outboxId}/bar") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("\"Malformed ID: bar\"", response.bodyAsText())
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
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `delete file rejects invalid folder ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.delete("$baseUrl/files/foo/${getNewRandomId()}") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("\"Malformed ID: foo\"", response.bodyAsText())
    }

    @Test
    fun `delete file rejects invalid file ID`(): Unit = runBlocking {
        val response: HttpResponse = httpClient.delete("$baseUrl/files/${contact1.outboxId}/bar") {
            authenticateWithToken(ownerToken)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("\"Malformed ID: bar\"", response.bodyAsText())
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
        assertEquals("""{"folders":[]}""", response.bodyAsText())
        // owner connection got registered
        assertTimestampRecent(metadataManager.ownerConnectionTime.value)
    }

    @Test
    fun `list folders returns more than a single folder`(): Unit = runBlocking {
        assertEquals(0L, metadataManager.ownerConnectionTime.value)
        // contact1 uploads a file
        val response1: HttpResponse = httpClient.post("$baseUrl/files/${contact1.outboxId}") {
            authenticateWithToken(contact1.token)
            setBody(bytes)
        }
        assertEquals(HttpStatusCode.OK, response1.status)

        // contact2 uploads a file
        val response2: HttpResponse = httpClient.post("$baseUrl/files/${contact2.outboxId}") {
            authenticateWithToken(contact2.token)
            setBody(bytes)
        }
        assertEquals(HttpStatusCode.OK, response2.status)
        assertEquals(0L, metadataManager.ownerConnectionTime.value)

        // owner now sees both contacts' outboxes in folders list
        val folderListResponse: FolderListResponse = httpClient.get("$baseUrl/folders") {
            authenticateWithToken(ownerToken)
        }.body()
        val folderList = setOf(FolderResponse(contact1.outboxId), FolderResponse(contact2.outboxId))
        assertEquals(folderList, folderListResponse.folders.toSet())
        // owner connection got registered
        assertTimestampRecent(metadataManager.ownerConnectionTime.value)
    }

    @Test
    fun `post new file and delete it once stale`(): Unit = runBlocking {
        // owner uploads the file
        val response: HttpResponse = httpClient.post("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(ownerToken)
            setBody(bytes)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertNumFiles(1)

        // contact can list the file
        val listResponse: HttpResponse = httpClient.get("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(contact1.token)
        }
        assertEquals(1, listResponse.body<FileListResponse>().files.size)

        // delete stale files
        testComponent.getFileManager().deleteStaleFiles(DAYS.toMillis(1))

        // contact can still list the file, because it wasn't stale
        val listResponse2: HttpResponse = httpClient.get("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(contact1.token)
        }
        assertEquals(1, listResponse2.body<FileListResponse>().files.size)

        // delete stale files again
        testComponent.getFileManager().deleteStaleFiles(1)

        // file was deleted, because it was stale
        val listResponse3: HttpResponse = httpClient.get("$baseUrl/files/${contact1.inboxId}") {
            authenticateWithToken(contact1.token)
        }
        assertEquals(0, listResponse3.body<FileListResponse>().files.size)

        assertNoTmpFiles()
        assertNumFiles(0)
    }

    private fun assertNoTmpFiles() {
        val dir = requireNotNull(this.tempDir)
        val tmp = File(dir, "tmp")
        assertTrue(tmp.isDirectory)
        assertEquals(0, tmp.listFiles()?.size)
    }

    private fun assertNumFiles(numFiles: Int) {
        assertTrue(fileProvider.folderRoot.isDirectory)
        var count = 0
        fileProvider.folderRoot.listFiles()?.forEach { folder ->
            count += folder.listFiles()?.size ?: 0
        }
        assertEquals(numFiles, count)
    }
}
