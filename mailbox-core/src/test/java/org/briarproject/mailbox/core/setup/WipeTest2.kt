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
    fun `concurrent wipe requests don't interfere and database access doesn't cause deadlocks`() {
        // One of the two dropper threads will succeed, the other will fail.
        val t1 = thread(name = "dropper 1") {
            try {
                wipeManager.wipeDatabaseAndFiles()
                incrementSuccess()
            } catch (t: Throwable) {
                incrementFailure()
            }
        }
        val t2 = thread(name = "dropper 2") {
            try {
                wipeManager.wipeDatabaseAndFiles()
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

        // reset field for exceptions thrown on background threads as this can indeed happen here
        // and is OK
        exceptionInBackgroundThread = null
    }

    @AfterEach
    override fun afterEach() {
        // We need to pass false here because calling wipeDatabaseAndFiles() with a closed Database
        // throws a DbClosedException.
        afterEach(false)
    }

}
