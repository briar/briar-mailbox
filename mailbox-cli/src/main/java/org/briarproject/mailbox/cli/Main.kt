package org.briarproject.mailbox.cli

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.briarproject.mailbox.core.CoreEagerSingletons
import org.briarproject.mailbox.core.JavaCliEagerSingletons
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.setup.QrCodeEncoder
import org.briarproject.mailbox.core.setup.SetupManager
import org.slf4j.LoggerFactory.getLogger
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
        default = false
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

    @Inject
    internal lateinit var db: Database

    @Inject
    internal lateinit var setupManager: SetupManager

    @Inject
    internal lateinit var qrCodeEncoder: QrCodeEncoder

    override fun run() {
        // logging
        val levelSlf4j = if (debug) Level.DEBUG else when (verbosity) {
            0 -> Level.WARN
            1 -> Level.INFO
            else -> Level.DEBUG
        }
        val level = if (debug) ALL else when (verbosity) {
            0 -> WARNING
            1 -> INFO
            else -> ALL
        }
        (getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = levelSlf4j
        LogManager.getLogManager().getLogger("").level = level

        getLogger(this.javaClass).debug("Hello Mailbox")
        println("Hello Mailbox")

        val javaCliComponent = DaggerJavaCliComponent.builder().build()
        javaCliComponent.inject(this)

        Runtime.getRuntime().addShutdownHook(
            Thread {
                lifecycleManager.stopServices()
                lifecycleManager.waitForShutdown()
            }
        )

        lifecycleManager.startServices()
        lifecycleManager.waitForStartup()

        // TODO this is obviously not the final code, just a stub to get us started
        val setupTokenExists = db.transactionWithResult(true) { txn ->
            setupManager.getSetupToken(txn) != null
        }
        val ownerTokenExists = db.transactionWithResult(true) { txn ->
            setupManager.getOwnerToken(txn) != null
        }
        if (!setupTokenExists && !ownerTokenExists) setupManager.restartSetup()
        qrCodeEncoder.getQrCodeBitMatrix()?.let {
            println(QrCodeRenderer.getQrString(it))
        }
    }

}

fun main(args: Array<String>) = Main().main(args)
