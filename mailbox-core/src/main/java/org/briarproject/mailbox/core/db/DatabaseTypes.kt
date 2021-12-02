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

package org.briarproject.mailbox.core.db

class DatabaseTypes(
    private val hashType: String,
    private val secretType: String,
    private val binaryType: String,
    private val counterType: String,
    private val stringType: String,
) {
    /**
     * Replaces database type placeholders in a statement with the actual types.
     * These placeholders are currently supported:
     *  *  _HASH
     *  *  _SECRET
     *  *  _BINARY
     *  *  _COUNTER
     *  *  _STRING
     */
    fun replaceTypes(stmt: String): String {
        var s = stmt
        s = s.replace("_HASH".toRegex(), hashType)
        s = s.replace("_SECRET".toRegex(), secretType)
        s = s.replace("_BINARY".toRegex(), binaryType)
        s = s.replace("_COUNTER".toRegex(), counterType)
        s = s.replace("_STRING".toRegex(), stringType)
        return s
    }
}
