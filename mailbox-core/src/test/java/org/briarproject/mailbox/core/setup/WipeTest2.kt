package org.briarproject.mailbox.core.setup

import org.briarproject.mailbox.core.server.IntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread
import kotlin.test.assertEquals

class WipeTest2 : IntegrationTest() {

    var failed = 0
    var succeeded = 0

    @Synchronized
    fun incrementFailure() {
        failed += 1
    }

    @Synchronized
    fun incrementSuccess() {
        succeeded += 1
    }

    @Test
    fun test() {
        // One of the two dropper threads will succeed, the other will fail.
        val t1 = thread(name = "dropper 1") {
            try {
                db.dropAllTablesAndClose()
                incrementSuccess()
            } catch (t: Throwable) {
                incrementFailure()
            }
        }
        val t2 = thread(name = "dropper 2") {
            try {
                db.dropAllTablesAndClose()
                incrementSuccess()
            } catch (t: Throwable) {
                incrementFailure()
            }
        }
        // This most likely throws a DbException within the thread, but if it does is doesn't make
        // the test fail.
        val t3 = thread(name = "other") {
            addOwnerToken()
        }
        t1.join()
        t2.join()
        t3.join()
        assertEquals(1, succeeded)
        assertEquals(1, failed)
    }

    @AfterEach
    override fun clearDb() {
        // This is not expected to work because calling dropAllTablesAndClose() on a closed Database
        // throws a DbClosedException.
        // super.clearDb()
    }

}
