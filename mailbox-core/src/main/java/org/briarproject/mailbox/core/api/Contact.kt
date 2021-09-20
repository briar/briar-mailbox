package org.briarproject.mailbox.core.api

data class Contact(
    val id: Int,
    val token: String,
    val inboxId: String,
    val outboxId: String,
)
