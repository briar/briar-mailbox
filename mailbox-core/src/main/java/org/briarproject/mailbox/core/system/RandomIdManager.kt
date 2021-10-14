package org.briarproject.mailbox.core.system

import java.security.SecureRandom
import javax.inject.Inject

private const val ID_SIZE = 32
private const val ID_HEX_SIZE = ID_SIZE * 2

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

}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte ->
    "%02x".format(eachByte)
}
