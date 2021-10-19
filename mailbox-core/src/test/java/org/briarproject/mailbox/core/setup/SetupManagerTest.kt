package org.briarproject.mailbox.core.setup

import org.briarproject.mailbox.core.server.IntegrationTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SetupManagerTest : IntegrationTest() {

    private val db by lazy { testComponent.getDatabase() }
    private val setupManager by lazy { testComponent.getSetupManager() }

    @Test
    fun `restarting setup wipes owner token`() {
        // initially, there's no owner token
        val initialOwnerToken = db.transactionWithResult(true) { txn ->
            setupManager.getOwnerToken(txn)
        }
        assertNull(initialOwnerToken)

        // setting an owner token stores it in DB
        setupManager.setOwnerToken(ownerToken)
        val firstOwnerToken = db.transactionWithResult(true) { txn ->
            setupManager.getOwnerToken(txn)
        }
        assertEquals(ownerToken, firstOwnerToken)

        // restarting setup wipes owner token
        setupManager.restartSetup()
        val wipedOwnerToken = db.transactionWithResult(true) { txn ->
            setupManager.getOwnerToken(txn)
        }
        assertNull(wipedOwnerToken)
    }

}