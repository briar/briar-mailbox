package org.briarproject.mailbox.lib

import org.briarproject.mailbox.core.util.LogUtils.info
import java.io.File

class ProductionMailbox(mailboxDir: File? = null) : Mailbox(mailboxDir) {

    override fun init() {
        LOG.info { "Hello Mailbox" }
        val javaLibComponent = DaggerJavaLibProductionComponent.builder()
            .javaLibModule(JavaLibModule(customDataDir)).build()
        javaLibComponent.inject(this)
    }

}
