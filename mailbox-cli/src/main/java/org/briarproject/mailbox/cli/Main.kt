/*
 *     Briar Mailbox
 *     Copyright (C) 2021-2022  The Briar Project
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.briarproject.mailbox.cli

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.briarproject.mailbox.core.CoreEagerSingletons
import org.briarproject.mailbox.core.JavaCliEagerSingletons
import org.briarproject.mailbox.core.db.TransactionManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.setup.QrCodeEncoder
import org.briarproject.mailbox.core.setup.SetupManager
import org.briarproject.mailbox.core.setup.WipeManager
import org.briarproject.mailbox.core.system.InvalidIdException
import org.slf4j.LoggerFactory.getLogger
import java.util.logging.Level.ALL
import java.util.logging.Level.INFO
import java.util.logging.Level.WARNING
import java.util.logging.LogManager
import javax.inject.Inject
import kotlin.system.exitProcess

class Main : CliktCommand(
    name = "briar-mailbox",
    help = "Command line interface for the Briar Mailbox"
) {
    private val wipe by option(
        "--wipe",
        help = "Deletes entire mailbox, will require new setup",
    ).flag(default = false)
    private val debug by option("--debug", "-d", help = "Enable printing of debug messages").flag(
        default = false
    )
    private val verbosity by option(
        "--verbose",
        "-v",
        help = "Print verbose log messages"
    ).counted()
    private val setupToken: String? by option("--setup-token", hidden = true)

    @Inject
    internal lateinit var coreEagerSingletons: CoreEagerSingletons

    @Inject
    internal lateinit var javaCliEagerSingletons: JavaCliEagerSingletons

    @Inject
    internal lateinit var lifecycleManager: LifecycleManager

    @Inject
    internal lateinit var db: TransactionManager

    @Inject
    internal lateinit var setupManager: SetupManager

    @Inject
    internal lateinit var wipeManager: WipeManager

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

        // This is a cli app, we'll always want this fully up when started, so start lifecycle
        lifecycleManager.startServices()
        lifecycleManager.waitForStartup()

        if (wipe) {
            // FIXME this can cause a deadlock
            //  see: https://code.briarproject.org/briar/briar-mailbox/-/issues/76
            val wipeResult = lifecycleManager.wipeMailbox()
            lifecycleManager.stopServices()
            lifecycleManager.waitForShutdown()
            if (wipeResult) {
                println("Mailbox wiped successfully \\o/")
                exitProcess(0)
            } else {
                println("ERROR: Mailbox was not wiped cleanly")
                exitProcess(1)
            }
        } else if (setupToken != null) {
            try {
                setupManager.setToken(setupToken, null)
            } catch (e: InvalidIdException) {
                System.err.println("Invalid setup token")
                exitProcess(1)
            }
        }

        val ownerTokenExists = db.read { txn ->
            setupManager.getOwnerToken(txn) != null
        }
        if (!ownerTokenExists) {
            // TODO remove before release
            val token = setupToken ?: db.read { setupManager.getSetupToken(it) }
            println("curl -v -H \"Authorization: Bearer $token\" -X PUT http://localhost:8000/setup")
            // FIXME: We need to wait for the hidden service address to become available
            // If not set up, show QR code for manual setup
            qrCodeEncoder.getQrCodeBitMatrix()?.let {
                println(QrCodeRenderer.getQrString(it))
            }
        }
    }

}

fun main(args: Array<String>) = Main().main(args)
