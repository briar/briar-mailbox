package org.briarproject.mailbox.core.setup

import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.files.FileManager
import org.briarproject.mailbox.core.server.AuthException
import org.briarproject.mailbox.core.server.MailboxPrincipal
import javax.inject.Inject

class WipeManager @Inject constructor(
    private val db: Database,
    private val fileManager: FileManager,
) {

    /**
     * Handler for `DELETE /` API endpoint.
     *
     * Wipes entire database as well as stored files
     * and returns `204 No Content` response if successful
     */
    @Throws(AuthException::class)
    suspend fun onWipeRequest(call: ApplicationCall) {
        val principal = call.principal<MailboxPrincipal>()
        if (principal !is MailboxPrincipal.OwnerPrincipal) throw AuthException()

        db.transaction(false) { txn ->
            db.clearDatabase(txn)
        }
        fileManager.deleteAllFiles()

        call.respond(HttpStatusCode.NoContent)
    }

}
