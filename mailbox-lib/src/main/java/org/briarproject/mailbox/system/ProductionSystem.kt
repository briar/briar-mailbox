package org.briarproject.mailbox.system

import org.briarproject.mailbox.core.system.System
import kotlin.system.exitProcess

internal class ProductionSystem : System {

    override fun exit(code: Int) {
        exitProcess(code)
    }
}
