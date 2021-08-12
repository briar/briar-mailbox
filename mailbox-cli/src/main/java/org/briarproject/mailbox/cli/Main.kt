package org.briarproject.mailbox.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.briarproject.mailbox.core.CoreEagerSingletons
import org.briarproject.mailbox.core.JavaCliEagerSingletons
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY
import java.lang.System.setProperty
import java.util.logging.Level.ALL
import java.util.logging.Level.INFO
import java.util.logging.Level.WARNING
import java.util.logging.LogManager
import javax.inject.Inject

class Main : CliktCommand(
    name = "briar-mailbox",
    help = "Command line interface for the Briar Mailbox"
) {
    private val debug by option("--debug", "-d", help = "Enable printing of debug messages").flag(
        default = true//false
    )
    private val verbosity by option(
        "--verbose",
        "-v",
        help = "Print verbose log messages"
    ).counted()

    @Inject
    internal lateinit var coreEagerSingletons: CoreEagerSingletons

    @Inject
    internal lateinit var javaCliEagerSingletons: JavaCliEagerSingletons

    @Inject
    internal lateinit var lifecycleManager: LifecycleManager

    override fun run() {
        // logging
        val levelSlf4j = if (debug) "DEBUG" else when (verbosity) {
            0 -> "WARN"
            1 -> "INFO"
            else -> "DEBUG"
        }
        val level = if (debug) ALL else when (verbosity) {
            0 -> WARNING
            1 -> INFO
            else -> ALL
        }
        setProperty(DEFAULT_LOG_LEVEL_KEY, levelSlf4j)
        LogManager.getLogManager().getLogger("").level = level

        println("Hello Mailbox")

        val javaCliComponent = DaggerJavaCliComponent.builder().build()
        javaCliComponent.inject(this)

        Runtime.getRuntime().addShutdownHook(Thread {
            lifecycleManager.stopServices()
            lifecycleManager.waitForShutdown()
        })

        lifecycleManager.startServices()
        lifecycleManager.waitForStartup()
    }

}

fun main(args: Array<String>) = Main().main(args)
