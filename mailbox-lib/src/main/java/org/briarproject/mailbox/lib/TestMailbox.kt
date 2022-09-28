package org.briarproject.mailbox.lib

import org.briarproject.mailbox.core.util.LogUtils.info
import org.briarproject.mailbox.system.TestSystem
import java.io.File
import javax.inject.Inject

class TestMailbox(mailboxDir: File? = null) : AbstractMailbox(mailboxDir) {

    init {
        LOG.info { "Hello Mailbox" }
        val mailboxLibComponent = DaggerMailboxLibTestComponent.builder()
            .mailboxLibModule(MailboxLibModule(customDataDir)).build()
        mailboxLibComponent.inject(this)
    }

    @Inject
    internal lateinit var testSystem: TestSystem

    fun hasExited(): Boolean {
        return testSystem.hasExited()
    }

    fun getExitCode(): Int {
        return testSystem.getExitCode()
    }
}
