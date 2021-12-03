package org.briarproject.mailbox.core.contacts

import com.fasterxml.jackson.core.JacksonException
import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.features.BadRequestException
import io.ktor.features.UnsupportedMediaTypeException
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.request.receive
import io.ktor.response.respond
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.server.AuthManager
import org.briarproject.mailbox.core.system.RandomIdManager
import org.briarproject.mailbox.core.util.LogUtils.logException
import org.slf4j.LoggerFactory.getLogger
import javax.inject.Inject

class ContactsManager @Inject constructor(
    private val db: Database,
    private val authManager: AuthManager,
    private val randomIdManager: RandomIdManager,
) {

    companion object {
        private val LOG = getLogger(ContactsManager::class.java)
    }

    /**
     * Used by owner to list contacts managed by the mailbox.
     *
     * Checks if provided auth token is the owner.
     * Responds with 200 (OK) with the list of contact IDs in JSON.
     */
    suspend fun listContacts(call: ApplicationCall) {
        authManager.assertIsOwner(call.principal())

        val contacts = db.read { txn ->
            db.getContacts(txn)
        }
        val contactIds = contacts.map { contact -> contact.contactId }
        call.respond(ContactsResponse(contactIds))
    }

    /**
     * Used by owner to add a new contact to the mailbox.
     *
     * Briar generates 32 bytes of random data for bearer token, inboxId and outboxId, encodes
     * them as hexadecimal strings and sends them along with its contactId.
     *
     * Checks if provided auth token is the owner.
     * Responds with 201 (CREATED) for a successful POST. If the contactId is already in use,
     * 409 (CONFLICT) is returned.
     */
    suspend fun postContact(call: ApplicationCall) {
        authManager.assertIsOwner(call.principal())
        val c: Contact = try {
            call.receive()
        } catch (e: JacksonException) {
            logException(LOG, e)
            throw BadRequestException("Unable to deserialise Contact: ${e.message}", e)
        } catch (e: UnsupportedMediaTypeException) {
            logException(LOG, e)
            throw BadRequestException("Unable to deserialise Contact: ${e.message}", e)
        }

        randomIdManager.assertIsRandomId(c.token)
        randomIdManager.assertIsRandomId(c.inboxId)
        randomIdManager.assertIsRandomId(c.outboxId)

        val status = db.write { txn ->
            if (db.getContact(txn, c.contactId) != null) {
                Conflict
            } else {
                db.addContact(txn, c)
                Created
            }
        }
        call.response.status(status)
    }

    /**
     * Used by owner to remove a contact.
     *
     * [paramContactId] is the integer contact ID the contact was added with.
     * Returns 200 (OK) when deletion was successful.
     */
    fun deleteContact(call: ApplicationCall, paramContactId: String) {
        authManager.assertIsOwner(call.principal())

        val contactId = try {
            Integer.parseInt(paramContactId)
        } catch (e: NumberFormatException) {
            throw BadRequestException("Invalid value for parameter contactId")
        }

        val status = db.write { txn ->
            if (db.getContact(txn, contactId) == null) {
                NotFound
            } else {
                db.removeContact(txn, contactId)
                OK
            }
        }
        call.response.status(status)
    }

}

data class ContactsResponse(val contacts: List<Int>)
