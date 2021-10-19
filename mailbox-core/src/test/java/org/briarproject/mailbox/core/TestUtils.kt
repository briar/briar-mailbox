package org.briarproject.mailbox.core

import io.mockk.every
import io.mockk.mockk
import org.briarproject.mailbox.core.contacts.Contact
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.db.Transaction
import org.briarproject.mailbox.core.system.ID_SIZE
import org.briarproject.mailbox.core.system.toHex
import org.briarproject.mailbox.core.util.IoUtils
import java.io.File
import kotlin.random.Random

object TestUtils {

    fun deleteTestDirectory(testDir: File) {
        IoUtils.deleteFileOrDir(testDir)
        testDir.parentFile.delete() // Delete if empty
    }

    fun getNewRandomId(): String = Random.nextBytes(ID_SIZE).toHex()

    fun getNewRandomContact(id: Int = Random.nextInt(1, Int.MAX_VALUE)) = Contact(
        contactId = id,
        token = getNewRandomId(),
        inboxId = getNewRandomId(),
        outboxId = getNewRandomId(),
    )

    /**
     * Allows you to mock [Database] access happening within a [Transaction] more comfortably.
     * Calls to [Database.transactionWithResult] will be mocked.
     * The given lambda [block] will get captured and invoked.
     */
    fun <T> everyTransactionWithResult(db: Database, readOnly: Boolean, block: (Transaction) -> T) {
        val txn = Transaction(mockk(), readOnly)
        every { db.transactionWithResult<T>(true, captureLambda()) } answers {
            lambda<(Transaction) -> T>().captured.invoke(txn)
        }
        block(txn)
    }

}
