package org.briarproject.mailbox.core.setup

import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.db.DatabaseConfig
import org.briarproject.mailbox.core.files.FileManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.server.AuthException
import org.briarproject.mailbox.core.server.MailboxPrincipal
import org.briarproject.mailbox.core.server.MailboxPrincipal.OwnerPrincipal
import org.briarproject.mailbox.core.util.IoUtils
import javax.inject.Inject

class WipeManager @Inject constructor(
    private val db: Database,
    private val databaseConfig: DatabaseConfig,
    private val fileManager: FileManager,
) {

    /*
     * This must only be called by the LifecycleManager
     */
    fun wipe(wipeDatabase: Boolean) {
        if (wipeDatabase) {
            db.dropAllTablesAndClose()
            val dir = databaseConfig.getDatabaseDirectory()
            IoUtils.deleteFileOrDir(dir)
        }
        fileManager.deleteAllFiles()
    }

}

class WipeRouteManager @Inject constructor(
    private val lifecycleManager: LifecycleManager,
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
        if (principal !is OwnerPrincipal) throw AuthException()

        val wiped = lifecycleManager.wipeMailbox()
        if (!wiped) {
            call.respond(HttpStatusCode.InternalServerError)
            return
        }

        call.respond(HttpStatusCode.NoContent)
    }

}
