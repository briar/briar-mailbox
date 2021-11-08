package org.briarproject.mailbox.core.files

import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveStream
import io.ktor.response.respond
import io.ktor.response.respondFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.server.AuthException
import org.briarproject.mailbox.core.server.AuthManager
import org.briarproject.mailbox.core.server.MailboxPrincipal
import org.briarproject.mailbox.core.system.InvalidIdException
import org.briarproject.mailbox.core.system.RandomIdManager
import org.slf4j.LoggerFactory.getLogger
import javax.inject.Inject

private val LOG = getLogger(FileManager::class.java)

class FileManager @Inject constructor(
    private val db: Database,
    private val authManager: AuthManager,
    private val fileProvider: FileProvider,
    private val randomIdManager: RandomIdManager,
) {

    /**
     * Used by contacts to send files to the owner and by the owner to send files to contacts.
     *
     * Checks if the authenticated [MailboxPrincipal] is allowed to upload to given [folderId],
     * Responds with 200 (OK) if upload was successful
     * (no 201 as the uploader doesn't need to know the $fileId)
     * The mailbox chooses a random ID string for the file ID.
     */
    @Throws(AuthException::class, InvalidIdException::class)
    suspend fun postFile(call: ApplicationCall, folderId: String) {
        val principal: MailboxPrincipal? = call.principal()
        randomIdManager.assertIsRandomId(folderId)
        authManager.assertCanPostToFolder(principal, folderId)

        val fileId = randomIdManager.getNewRandomId()
        withContext(Dispatchers.IO) {
            val tmpFile = fileProvider.getTemporaryFile(fileId)
            tmpFile.outputStream().use { outputStream ->
                call.receiveStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            val file = fileProvider.getFile(folderId, fileId)
            if (!tmpFile.renameTo(file)) error("Error moving file")
        }

        call.respond(HttpStatusCode.OK)
    }

    /**
     * Used by owner and contacts to list their files to retrieve.
     *
     * Checks if the authenticated [MailboxPrincipal] is allowed to download from [folderId].
     * Responds with 200 (OK) with the list of files in JSON.
     */
    suspend fun listFiles(call: ApplicationCall, folderId: String) {
        val principal: MailboxPrincipal? = call.principal()
        randomIdManager.assertIsRandomId(folderId)
        authManager.assertCanDownloadFromFolder(principal, folderId)

        val fileListResponse = withContext(Dispatchers.IO) {
            val list = ArrayList<FileResponse>()
            fileProvider.getFolder(folderId).listFiles()?.forEach { file ->
                list.add(FileResponse(file.name, file.lastModified()))
            }
            FileListResponse(list)
        }
        call.respond(HttpStatusCode.OK, fileListResponse)
    }

    /**
     * Used by owner and contacts to retrieve a file.
     *
     * Checks if the authenticated [MailboxPrincipal] is allowed to download from $folderId
     * Returns 200 (OK) if successful with the files' bytes in the response body
     */
    @Throws(AuthException::class, InvalidIdException::class)
    suspend fun getFile(call: ApplicationCall, folderId: String, fileId: String) {
        val principal: MailboxPrincipal? = call.principal()
        randomIdManager.assertIsRandomId(folderId)
        randomIdManager.assertIsRandomId(fileId)
        authManager.assertCanDownloadFromFolder(principal, folderId)

        val file = fileProvider.getFile(folderId, fileId)
        if (file.isFile) call.respondFile(file)
        else call.respond(HttpStatusCode.NotFound)
    }

    /**
     * Used by owner and contacts to delete files.
     *
     * Checks if the authenticated [MailboxPrincipal] is allowed to download from [folderId].
     * Responds with 200 (OK) if deletion was successful.
     */
    suspend fun deleteFile(call: ApplicationCall, folderId: String, fileId: String) {
        val principal: MailboxPrincipal? = call.principal()
        randomIdManager.assertIsRandomId(folderId)
        randomIdManager.assertIsRandomId(fileId)
        authManager.assertCanDownloadFromFolder(principal, folderId)

        val file = fileProvider.getFile(folderId, fileId)
        if (file.isFile) {
            if (file.delete()) call.respond(HttpStatusCode.OK)
            else call.respond(HttpStatusCode.InternalServerError)
        } else call.respond(HttpStatusCode.NotFound)
    }

    /**
     * Used by owner only to list all folders that have files available for download.
     *
     * Checks if provided auth token is the owner.
     * Responds with 200 (OK) with the list of folders with files in JSON.
     */
    suspend fun listFoldersWithFiles(call: ApplicationCall) {
        val principal: MailboxPrincipal? = call.principal()
        authManager.assertIsOwner(principal)

        val folderListResponse = withContext(Dispatchers.IO) {
            val list = ArrayList<FolderResponse>()
            val contacts = db.read { txn -> db.getContacts(txn) }
            contacts.forEach { c ->
                val id = c.outboxId
                val folder = fileProvider.getFolder(id)
                if (folder.listFiles()?.isNotEmpty() == true) {
                    list.add(FolderResponse(id))
                }
            }
            FolderListResponse(list)
        }
        call.respond(folderListResponse)
    }

    fun deleteAllFiles(): Boolean {
        var allDeleted = true
        fileProvider.folderRoot.listFiles()?.forEach { folder ->
            if (!folder.deleteRecursively()) {
                allDeleted = false
                LOG.warn("Not everything in $folder could get deleted.")
            }
        } ?: run {
            allDeleted = false
            LOG.warn("Could not delete folders.")
        }
        return allDeleted
    }
}

data class FileListResponse(val files: List<FileResponse>)
data class FileResponse(val name: String, val time: Long)
data class FolderListResponse(val folders: List<FolderResponse>)
data class FolderResponse(val id: String)
