package org.briarproject.mailbox.core.db

import org.briarproject.mailbox.core.TestUtils.deleteTestDirectory
import org.briarproject.mailbox.core.api.Contact
import org.briarproject.mailbox.core.settings.Settings
import org.briarproject.mailbox.core.system.Clock
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

abstract class JdbcDatabaseTest {

    @TempDir
    lateinit var testDir: File

    protected abstract fun createDatabase(
        config: DatabaseConfig,
        clock: Clock,
    ): JdbcDatabase

    @Throws(java.lang.Exception::class)
    fun open(
        resume: Boolean,
    ): Database {
        val db: Database = createDatabase(
            TestDatabaseConfig(testDir)
        ) { System.currentTimeMillis() }
        if (!resume) deleteTestDirectory(testDir)
        db.open(null)
        return db
    }

    @Test
    @Throws(Exception::class)
    open fun testPersistence() {
        // Store some records
        val contact1 = Contact(
            1,
            "4291ad1d-897d-4db4-9de9-ea3f78c5262e",
            "f21467bd-afb0-4c0e-9090-cae45ea1eae9",
            "880629fb-3226-41d8-a978-7b28cf44d57d"
        )
        val contact2 = Contact(
            2,
            "fbbe9a63-2f28-46d4-a465-e6ca57a5d811",
            "7931fa7a-077e-403a-8487-63261027d6d2",
            "12a61ca3-af0a-41d1-acc1-a0f4625f6e42"
        )
        var db: Database = open(false)
        db.transaction(false) { txn ->

            db.addContact(txn, contact1)
            db.addContact(txn, contact2)
        }
        db.close()

        // Check that the records are still there
        db = open(true)
        db.transaction(false) { txn ->
            val contact1Reloaded1 = db.getContact(txn, 1)
            val contact2Reloaded1 = db.getContact(txn, 2)
            assertEquals(contact1, contact1Reloaded1)
            assertEquals(contact2, contact2Reloaded1)

            // Delete one of the records
            db.removeContact(txn, 1)
        }
        db.close()

        // Check that the record is gone
        db = open(true)
        db.transaction(true) { txn ->
            val contact1Reloaded2 = db.getContact(txn, 1)
            val contact2Reloaded2 = db.getContact(txn, 2)
            assertNull(contact1Reloaded2)
            assertEquals(contact2, contact2Reloaded2)
        }
        db.close()
    }

    @Test
    @Throws(java.lang.Exception::class)
    open fun testMergeSettings() {
        val before = Settings()
        before["foo"] = "bar"
        before["baz"] = "bam"
        val update = Settings()
        update["baz"] = "qux"
        val merged = Settings()
        merged["foo"] = "bar"
        merged["baz"] = "qux"

        var db: Database = open(false)
        var txn = db.transaction(false) { txn ->
            // store 'before'
            db.mergeSettings(txn, before, "namespace")
            assertEquals(before, db.getSettings(txn, "namespace"))

            // merge 'update'
            db.mergeSettings(txn, update, "namespace")
            assertEquals(merged, db.getSettings(txn, "namespace"))
        }
        db.close()
    }

}
