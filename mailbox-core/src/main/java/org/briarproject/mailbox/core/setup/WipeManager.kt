/*
 *     Briar Mailbox
 *     Copyright (C) 2021-2022  The Briar Project
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.briarproject.mailbox.core.setup

import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.files.FileManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.server.AuthException
import org.briarproject.mailbox.core.server.MailboxPrincipal
import org.briarproject.mailbox.core.server.MailboxPrincipal.OwnerPrincipal
import javax.inject.Inject

class WipeManager @Inject constructor(
    private val db: Database,
    private val fileManager: FileManager,
) {

    /**
     * Drops database tables and then deletes all files, includes the database files.
     *
     * This must only be called by the [LifecycleManager].
     */
    fun wipeDatabaseAndFiles() {
        db.dropAllTablesAndClose()
        fileManager.deleteAllFiles()
    }

    /**
     * Deletes all files, includes the database files.
     *
     * This must only be called by the [LifecycleManager]
     * or by the CLI when no lifecycle was started.
     */
    fun wipeFilesOnly() {
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
