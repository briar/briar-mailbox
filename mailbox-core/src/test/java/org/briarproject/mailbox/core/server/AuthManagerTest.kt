package org.briarproject.mailbox.core.server

import io.mockk.every
import io.mockk.mockk
import org.briarproject.mailbox.core.TestUtils.everyTransactionWithResult
import org.briarproject.mailbox.core.TestUtils.getNewRandomContact
import org.briarproject.mailbox.core.TestUtils.getNewRandomId
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.server.MailboxPrincipal.OwnerPrincipal
import org.briarproject.mailbox.core.setup.SetupManager
import org.briarproject.mailbox.core.system.InvalidIdException
import org.briarproject.mailbox.core.system.RandomIdManager
import org.briarproject.mailbox.core.system.toHex
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthManagerTest {

    private val db: Database = mockk()
    private val setupManager: SetupManager = mockk()
    private val randomIdManager = RandomIdManager()

    private val authManager = AuthManager(db, setupManager, randomIdManager)

    private val id = getNewRandomId()
    private val otherId = getNewRandomId()
    private val invalidId = Random.nextBytes(Random.nextInt(0, 32)).toHex()
    private val contact = getNewRandomContact()
    private val contactPrincipal = MailboxPrincipal.ContactPrincipal(contact)

    @Test
    fun `rejects invalid token for getPrincipal()`() {
        assertThrows<InvalidIdException> {
            authManager.getPrincipal(invalidId)
        }
    }

    @Test
    fun `getPrincipal() returns authenticated contact`() {
        everyTransactionWithResult(db, true) { txn ->
            every { db.getContactWithToken(txn, id) } returns contactPrincipal.contact
        }
        assertEquals(contactPrincipal, authManager.getPrincipal(id))
    }

    @Test
    fun `getPrincipal() returns authenticated owner`() {
        everyTransactionWithResult(db, true) { txn ->
            every { db.getContactWithToken(txn, id) } returns null
            every { setupManager.getOwnerToken(txn) } returns id
        }

        assertEquals(OwnerPrincipal, authManager.getPrincipal(id))
    }

    @Test
    fun `getPrincipal() returns null when unauthenticated`() {
        everyTransactionWithResult(db, true) { txn ->
            every { db.getContactWithToken(txn, id) } returns null
            every { setupManager.getOwnerToken(txn) } returns otherId
        }

        assertNull(authManager.getPrincipal(id))
    }

    @Test
    fun `assertCanDownloadFromFolder() throws for null MailboxPrincipal`() {
        assertThrows<AuthException> {
            authManager.assertCanDownloadFromFolder(null, id)
        }
    }

    @Test
    fun `assertCanDownloadFromFolder() throws if owner wants non-existent folder`() {
        everyTransactionWithResult(db, true) { txn ->
            every { db.getContacts(txn) } returns emptyList()
        }

        assertThrows<AuthException> {
            authManager.assertCanDownloadFromFolder(OwnerPrincipal, id)
        }
    }

    @Test
    fun `throws if contact wants to download from folder that is not their inbox`() {
        assertThrows<AuthException> {
            authManager.assertCanDownloadFromFolder(contactPrincipal, id)
        }
        assertThrows<AuthException> {
            authManager.assertCanDownloadFromFolder(contactPrincipal, contact.outboxId)
        }
    }

    @Test
    fun `assertCanDownloadFromFolder() lets owner access contact's outbox folder`() {
        everyTransactionWithResult(db, true) { txn ->
            every { db.getContacts(txn) } returns listOf(contact, getNewRandomContact())
        }

        authManager.assertCanDownloadFromFolder(OwnerPrincipal, contact.outboxId)
    }

    @Test
    fun `assertCanDownloadFromFolder() lets contact access their inbox folder`() {
        authManager.assertCanDownloadFromFolder(contactPrincipal, contact.inboxId)
    }

    @Test
    fun `assertCanPostToFolder() throws for null MailboxPrincipal`() {
        assertThrows<AuthException> {
            authManager.assertCanPostToFolder(null, id)
        }
    }

    @Test
    fun `assertCanPostToFolder() throws if owner wants non-existent folder`() {
        everyTransactionWithResult(db, true) { txn ->
            every { db.getContacts(txn) } returns emptyList()
        }

        assertThrows<AuthException> {
            authManager.assertCanPostToFolder(OwnerPrincipal, id)
        }
    }

    @Test
    fun `throws if contact wants to post to folder that is not their outbox`() {
        assertThrows<AuthException> {
            authManager.assertCanPostToFolder(contactPrincipal, id)
        }
        assertThrows<AuthException> {
            authManager.assertCanPostToFolder(contactPrincipal, contact.inboxId)
        }
    }

    @Test
    fun `assertCanPostToFolder() lets owner access contact's inbox folder`() {
        everyTransactionWithResult(db, true) { txn ->
            every { db.getContacts(txn) } returns listOf(contact, getNewRandomContact())
        }

        authManager.assertCanPostToFolder(OwnerPrincipal, contact.inboxId)
    }

    @Test
    fun `assertCanPostToFolder() lets contact access their outbox folder`() {
        authManager.assertCanPostToFolder(contactPrincipal, contact.outboxId)
    }

    @Test
    fun `assertIsOwner() throws for non-owners`() {
        assertThrows<AuthException> { authManager.assertIsOwner(null) }
        assertThrows<AuthException> { authManager.assertIsOwner(contactPrincipal) }
    }

    @Test
    fun `assertIsOwner() passes for owner`() {
        authManager.assertIsOwner(OwnerPrincipal)
    }

}
