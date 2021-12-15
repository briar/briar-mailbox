package org.briarproject.mailbox.core.setup

import org.briarproject.mailbox.core.server.IntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

class WipeTest1 : IntegrationTest() {

    @Test
    fun test() {
        val t1 = thread(name = "dropper") {
            db.dropAllTablesAndClose()
        }
        // This most likely throws a DbException within the thread, but if it does is doesn't make
        // the test fail.
        val t2 = thread(name = "other") {
            addOwnerToken()
        }
        t1.join()
        t2.join()
    }

    @AfterEach
    override fun clearDb() {
        // This is not expected to work because calling dropAllTablesAndClose() on a closed Database
        // throws a DbClosedException.
        // super.clearDb()
    }

}
