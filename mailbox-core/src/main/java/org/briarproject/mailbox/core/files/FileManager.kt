package org.briarproject.mailbox.core.files

import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.server.AuthException
import org.briarproject.mailbox.core.server.AuthManager
import org.briarproject.mailbox.core.server.MailboxPrincipal
import org.briarproject.mailbox.core.system.InvalidIdException
import org.briarproject.mailbox.core.system.RandomIdManager
import javax.inject.Inject

class FileManager @Inject constructor(
    private val db: Database,
    private val authManager: AuthManager,
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

        // TODO implement

        call.respond(HttpStatusCode.OK, "post: Not yet implemented. folderId: $folderId}")
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

        // TODO implement

        call.respond(HttpStatusCode.OK, "get: Not yet implemented. folderId: $folderId")
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

        // TODO implement

        call.respond(
            HttpStatusCode.OK,
            "get: Not yet implemented. folderId: $folderId fileId: $fileId"
        )
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

        // TODO implement

        call.respond(
            HttpStatusCode.OK,
            "delete: Not yet implemented. folderId: $folderId fileId: $fileId"
        )
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

        // TODO implement

        call.respond(HttpStatusCode.OK, "get: Not yet implemented")
    }

}
