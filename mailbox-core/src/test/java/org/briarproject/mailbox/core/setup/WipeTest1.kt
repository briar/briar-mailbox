package org.briarproject.mailbox.core.setup

import org.briarproject.mailbox.core.server.IntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

class WipeTest1 : IntegrationTest() {

    @Test
    fun `wiping and concurrent database access doesn't cause deadlocks`() {
        val t1 = thread(name = "dropper") {
            wipeManager.wipeDatabaseAndFiles()
        }
        // This most likely throws a DbException within the thread, but if it does is doesn't make
        // the test fail.
        val t2 = thread(name = "other") {
            addOwnerToken()
        }
        t1.join()
        t2.join()

        // reset flag for exceptions thrown on background threads as this can indeed happen here
        // and is OK
        exceptionInBackgroundThread = false
    }

    @AfterEach
    override fun afterEach() {
        // We need to pass false here because calling wipeDatabaseAndFiles() with a closed Database
        // throws a DbClosedException.
        afterEach(false)
    }

}
