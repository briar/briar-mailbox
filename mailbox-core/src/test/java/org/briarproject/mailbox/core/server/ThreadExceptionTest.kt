package org.briarproject.mailbox.core.server

import org.junit.jupiter.api.Test
import kotlin.concurrent.thread
import kotlin.test.assertNotNull

class ThreadExceptionTest : IntegrationTest() {

    @Test
    fun `exception thrown on background thread gets caught and stored`() {
        val t = thread(name = "failing thread") {
            throw RuntimeException("boom")
        }
        t.join()

        assertNotNull(exceptionInBackgroundThread)
        exceptionInBackgroundThread = null
    }

}
