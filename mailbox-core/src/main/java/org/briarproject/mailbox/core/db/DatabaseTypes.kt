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
