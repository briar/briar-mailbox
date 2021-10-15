package org.briarproject.mailbox.core.system

import java.security.SecureRandom
import javax.inject.Inject

private const val ID_SIZE = 32
private const val ID_HEX_SIZE = ID_SIZE * 2

/**
 * Generates and validates random IDs
 * that are being used for auth tokens, folder IDs and file names.
 */
class RandomIdManager @Inject constructor() {

    private val secureRandom = SecureRandom()
    private val idRegex = Regex("[a-f0-9]{64}")

    fun getNewRandomId(): String {
        val idBytes = ByteArray(ID_SIZE)
        secureRandom.nextBytes(idBytes)
        return idBytes.toHex()
    }

    fun isValidRandomId(id: String): Boolean {
        if (id.length != ID_HEX_SIZE) return false
        return idRegex.matches(id)
    }

    @Throws(InvalidIdException::class)
    fun assertIsRandomId(id: String) {
        if (!isValidRandomId(id)) throw InvalidIdException(id)
    }

}

class InvalidIdException(val id: String) : IllegalStateException()

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte ->
    "%02x".format(eachByte)
}
