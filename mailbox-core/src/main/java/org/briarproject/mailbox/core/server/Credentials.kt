package org.briarproject.mailbox.core.server

import io.ktor.http.HttpMethod

data class Credentials(
    val accessType: AccessType,
    val token: String,
    val folderId: String?,
)

enum class AccessType { UPLOAD, DOWNLOAD_DELETE }

internal fun HttpMethod.toAccessType(): AccessType? = when (this) {
    HttpMethod.Get -> AccessType.DOWNLOAD_DELETE
    HttpMethod.Delete -> AccessType.DOWNLOAD_DELETE
    HttpMethod.Post -> AccessType.UPLOAD
    HttpMethod.Put -> AccessType.UPLOAD
    else -> null
}

internal object AuthContext {
    const val ownerOnly = "ownerOnly"
    const val ownerAndContacts = "ownerAndContacts"
}
