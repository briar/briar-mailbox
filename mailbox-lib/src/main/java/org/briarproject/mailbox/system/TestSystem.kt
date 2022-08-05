package org.briarproject.mailbox.system

import org.briarproject.mailbox.core.system.System

sealed interface Exited
internal object NotExited : Exited
internal class DidExit(val code: Int) : Exited

internal class TestSystem(private var exited: Exited = NotExited) : System {

    override fun exit(code: Int) {
        exited = DidExit(code)
    }

    fun hasExited(): Boolean {
        return exited is DidExit
    }

    fun getExitCode(): Int {
        check(exited is DidExit)
        return (exited as DidExit).code
    }
}
