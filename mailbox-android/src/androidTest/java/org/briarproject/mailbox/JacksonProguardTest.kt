package org.briarproject.mailbox

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.briarproject.mailbox.core.contacts.Contact
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JacksonProguardTest {

    private val mapper = ObjectMapper().apply { registerKotlinModule() }

    /**
     * Tests if we can deserialize our objects with Jackson while using proguard:
     * [issue](https://github.com/FasterXML/jackson-module-kotlin/issues/522)
     */
    @Test
    fun testDeserializeContact() {
        val json = """
            {
                 "contactId": 1,
                 "token": "foo",
                 "inboxId": "bar",
                 "outboxId": "42"
            }
        """.trimIndent()
        val contact: Contact = mapper.readValue(json)
        assertEquals(1, contact.contactId)
    }

}
