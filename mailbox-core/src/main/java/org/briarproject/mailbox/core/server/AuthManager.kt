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

package org.briarproject.mailbox.core.server

import io.ktor.server.auth.Principal
import org.briarproject.mailbox.core.contacts.Contact
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.server.MailboxPrincipal.ContactPrincipal
import org.briarproject.mailbox.core.server.MailboxPrincipal.OwnerPrincipal
import org.briarproject.mailbox.core.server.MailboxPrincipal.SetupPrincipal
import org.briarproject.mailbox.core.settings.MetadataManager
import org.briarproject.mailbox.core.setup.SetupManager
import org.briarproject.mailbox.core.system.RandomIdManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val db: Database,
    private val setupManager: SetupManager,
    private val metadataManager: MetadataManager,
    private val randomIdManager: RandomIdManager,
) {

    /**
     * Returns the principal the given token belongs to
     * or null if this token doesn't belong to any principal.
     */
    fun getPrincipal(token: String): MailboxPrincipal? {
        randomIdManager.assertIsRandomId(token)
        val principal = db.read { txn ->
            val contact = db.getContactWithToken(txn, token)
            when {
                contact != null -> ContactPrincipal(contact)
                setupManager.getOwnerToken(txn) == token -> OwnerPrincipal
                setupManager.getSetupToken(txn) == token -> SetupPrincipal
                else -> null
            }
        }
        // We register the owner connection here before further call validation.
        // It can still happen that the owner sends invalid requests, but that's fine here.
        if (principal is OwnerPrincipal) metadataManager.onOwnerConnected()
        return principal
    }

    /**
     * @throws [AuthException] when given [principal] is NOT allowed
     * to download or delete from the given [folderId] which is assumed to be validated already.
     */
    @Throws(AuthException::class)
    fun assertCanDownloadFromFolder(principal: MailboxPrincipal?, folderId: String) {
        if (principal == null) throw AuthException()

        if (principal is OwnerPrincipal) {
            val contacts = db.read { txn -> db.getContacts(txn) }
            val noOutboxFound = contacts.none { c -> folderId == c.outboxId }
            if (noOutboxFound) throw AuthException()
        } else if (principal is ContactPrincipal) {
            if (folderId != principal.contact.inboxId) throw AuthException()
        }
    }

    /**
     * @throws [AuthException] when given [principal] is NOT allowed
     * to post to the given [folderId] which is assumed to be validated already.
     */
    @Throws(AuthException::class)
    fun assertCanPostToFolder(principal: MailboxPrincipal?, folderId: String) {
        if (principal == null) throw AuthException()

        if (principal is OwnerPrincipal) {
            val contacts = db.read { txn -> db.getContacts(txn) }
            val noInboxFound = contacts.none { c -> folderId == c.inboxId }
            if (noInboxFound) throw AuthException()
        } else if (principal is ContactPrincipal) {
            if (folderId != principal.contact.outboxId) throw AuthException()
        }
    }

    /**
     * @throws [AuthException] when given [principal] is NOT the mailbox owner.
     */
    @Throws(AuthException::class)
    fun assertIsOwner(principal: MailboxPrincipal?) {
        if (principal !is OwnerPrincipal) throw AuthException()
    }

    /**
     * @throws [AuthException] when given [principal] is NOT the setup token.
     */
    @Throws(AuthException::class)
    fun assertIsSetup(principal: MailboxPrincipal?) {
        if (principal !is SetupPrincipal) throw AuthException()
        // setting up as SetupPrincipal counts as an owner connection
        metadataManager.onOwnerConnected()
    }

}

sealed class MailboxPrincipal : Principal {
    object SetupPrincipal : MailboxPrincipal()
    object OwnerPrincipal : MailboxPrincipal()
    data class ContactPrincipal(val contact: Contact) : MailboxPrincipal()
}

class AuthException : IllegalStateException()
