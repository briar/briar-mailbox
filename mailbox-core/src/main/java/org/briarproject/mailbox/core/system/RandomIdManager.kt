/*
 *     Briar Mailbox
 *     Copyright (C) 2021-2022  The Briar Project
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.briarproject.mailbox.core.system

import java.security.SecureRandom
import javax.inject.Inject

internal const val ID_SIZE = 32
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
