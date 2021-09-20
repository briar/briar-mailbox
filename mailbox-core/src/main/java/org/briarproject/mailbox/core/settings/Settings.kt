package org.briarproject.mailbox.core.settings

import java.util.Hashtable

class Settings : Hashtable<String, String>() {

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
