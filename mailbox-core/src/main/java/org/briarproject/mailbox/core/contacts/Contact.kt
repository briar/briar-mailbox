package org.briarproject.mailbox.core.contacts

data class Contact(
    val contactId: Int,
    val token: String,
    val inboxId: String,
    val outboxId: String,
)
