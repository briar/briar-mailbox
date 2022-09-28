package org.briarproject.mailbox.lib

import org.briarproject.mailbox.core.util.LogUtils.info
import org.briarproject.mailbox.system.TestSystem
import java.io.File
import javax.inject.Inject

class TestMailbox(mailboxDir: File? = null) : Mailbox(mailboxDir) {

    override fun init() {
        LOG.info { "Hello Mailbox" }
        val javaLibComponent = DaggerJavaLibTestComponent.builder()
            .javaLibModule(JavaLibModule(customDataDir)).build()
        javaLibComponent.inject(this)
    }

    @Inject
    internal lateinit var system: TestSystem

    fun hasExited(): Boolean {
        return system.hasExited()
    }

    fun getExitCode(): Int {
        return system.getExitCode()
    }
}
