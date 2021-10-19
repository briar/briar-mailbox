package org.briarproject.mailbox.core.server

import io.ktor.auth.Principal
import org.briarproject.mailbox.core.contacts.Contact
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.server.MailboxPrincipal.ContactPrincipal
import org.briarproject.mailbox.core.server.MailboxPrincipal.OwnerPrincipal
import org.briarproject.mailbox.core.setup.SetupManager
import org.briarproject.mailbox.core.system.RandomIdManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val db: Database,
    private val setupManager: SetupManager,
    private val randomIdManager: RandomIdManager,
) {

    /**
     * Returns the principal the given token belongs to
     * or null if this token doesn't belong to any principal.
     */
    fun getPrincipal(token: String): MailboxPrincipal? {
        randomIdManager.assertIsRandomId(token)
        return db.transactionWithResult(true) { txn ->
            val contact = db.getContactWithToken(txn, token)
            if (contact != null) {
                ContactPrincipal(contact)
            } else {
                if (token == setupManager.getOwnerToken(txn)) OwnerPrincipal
                else null
            }
        }
    }

    /**
     * @throws [AuthException] when given [principal] is NOT allowed
     * to download or delete from the given [folderId] which is assumed to be validated already.
     */
    @Throws(AuthException::class)
    fun assertCanDownloadFromFolder(principal: MailboxPrincipal?, folderId: String) {
        if (principal == null) throw AuthException()

        if (principal is OwnerPrincipal) {
            val contacts = db.transactionWithResult(true) { txn -> db.getContacts(txn) }
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
            val contacts = db.transactionWithResult(true) { txn -> db.getContacts(txn) }
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

}

sealed class MailboxPrincipal : Principal {
    object OwnerPrincipal : MailboxPrincipal()
    data class ContactPrincipal(val contact: Contact) : MailboxPrincipal()
}

class AuthException : IllegalStateException()
