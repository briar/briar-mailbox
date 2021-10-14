package org.briarproject.mailbox.core.server

import io.ktor.auth.Principal
import org.briarproject.mailbox.core.db.Database
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val db: Database,
) {

    /**
     * Returns the principal the given token belongs to
     * or null if this token doesn't belong to any principal.
     */
    fun getPrincipal(token: String): MailboxPrincipal? {
        // TODO get real principal owning token from DB or null of token unknown
        return MailboxPrincipal.Owner(token)
    }

    /**
     * @throws [AuthenticationException] when given [principal] is NOT allowed
     * to download or delete from the given [folderId].
     */
    @Throws(AuthenticationException::class)
    fun assertCanDownloadFromFolder(principal: MailboxPrincipal?, folderId: String) {
        if (principal == null) throw AuthenticationException()

        // TODO check access of principal to folderId
    }

    /**
     * @throws [AuthenticationException] when given [principal] is NOT allowed
     * to post to the given [folderId].
     */
    @Throws(AuthenticationException::class)
    fun assertCanPostToFolder(principal: MailboxPrincipal?, folderId: String) {
        if (principal == null) throw AuthenticationException()
        // TODO check access of principal to folderId
    }

    /**
     * @throws [AuthenticationException] when given [principal] is NOT the mailbox owner.
     */
    @Throws(AuthenticationException::class)
    fun assertIsOwner(principal: MailboxPrincipal?) {
        if (principal !is MailboxPrincipal.Owner) throw AuthenticationException()
    }

}

sealed class MailboxPrincipal(val token: String) : Principal {

    class Owner(token: String) : MailboxPrincipal(token)
    class Contact(token: String, val contactId: Int) : MailboxPrincipal(token)

}

class AuthenticationException : IllegalStateException()
