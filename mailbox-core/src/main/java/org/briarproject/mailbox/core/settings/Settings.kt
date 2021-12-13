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

package org.briarproject.mailbox.core.settings

import java.util.Hashtable

class Settings : Hashtable<String, String>() {

    /**
     * Note that null values will get stored as empty string.
     */
    override fun put(key: String, value: String?): String? {
        return if (value == null) super.put(key, "")
        else super.put(key, value)
    }

    /**
     * Note that empty strings get returned as null.
     */
    override fun get(key: String): String? {
        val value = super.get(key)
        return if (value.isNullOrEmpty()) null
        else value
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val s = get(key) ?: return defaultValue
        if ("true" == s) return true
        return if ("false" == s) false else defaultValue
    }

    fun putBoolean(key: String, value: Boolean) {
        put(key, value.toString())
    }

    fun getInt(key: String, defaultValue: Int): Int {
        val s = get(key) ?: return defaultValue
        return try {
            Integer.parseInt(s)
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    fun putInt(key: String, value: Int) {
        put(key, value.toString())
    }

    fun getLong(key: String, defaultValue: Long): Long {
        val s = get(key) ?: return defaultValue
        return try {
            java.lang.Long.parseLong(s)
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    fun putLong(key: String, value: Long) {
        put(key, value.toString())
    }
}
