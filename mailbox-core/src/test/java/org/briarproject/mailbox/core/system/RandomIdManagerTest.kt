package org.briarproject.mailbox.core.system

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RandomIdManagerTest {

    private val randomIdManager = RandomIdManager()

    @Test
    fun `generated IDs are considered valid`() {
        for (i in 0..23) {
            val id = randomIdManager.getNewRandomId()
            assertEquals(64, id.length)
            assertTrue(randomIdManager.isValidRandomId(id))
        }
    }

}
