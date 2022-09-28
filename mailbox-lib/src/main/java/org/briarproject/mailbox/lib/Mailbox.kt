package org.briarproject.mailbox.lib

import org.briarproject.mailbox.core.util.LogUtils.info
import java.io.File

class Mailbox(mailboxDir: File? = null) : AbstractMailbox(mailboxDir) {

    init {
        LOG.info { "Hello Mailbox" }
        val mailboxLibComponent = DaggerMailboxLibComponent.builder()
            .mailboxLibModule(MailboxLibModule(customDataDir)).build()
        mailboxLibComponent.inject(this)
    }
}
