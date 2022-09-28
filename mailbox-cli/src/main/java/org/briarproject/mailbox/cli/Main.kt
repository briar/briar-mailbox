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
import org.briarproject.mailbox.core.system.InvalidIdException
import org.briarproject.mailbox.lib.Mailbox
import org.slf4j.LoggerFactory.getLogger

class Main : CliktCommand(
    name = "briar-mailbox",
    help = "Command line interface for the Briar Mailbox"
) {
    private val wipe by option(
        "--wipe", help = "Deletes entire mailbox, will require new setup",
    ).flag(default = false)
    private val info by option(
        "--info", "-i", help = "Enable printing of info messages (alias for -v)"
    ).flag(default = false)
    private val debug by option(
        "--debug", "-d", help = "Enable printing of debug messages (alias for -vv)"
    ).flag(default = false)
    private val trace by option(
        "--trace", "-t", help = "Enable printing of trace messages (alias for -vvv)"
    ).flag(default = false)
    private val verbosity by option(
        "--verbose", "-v", help = "Print verbose log messages"
    ).counted()
    private val setupToken: String? by option("--setup-token", hidden = true)

    override fun run() {
        // logging
        val levelNamed = when {
            trace -> Level.TRACE
            debug -> Level.DEBUG
            info -> Level.INFO
            else -> Level.WARN
        }
        val levelVerbose = when (verbosity) {
            0 -> Level.WARN
            1 -> Level.INFO
            2 -> Level.DEBUG
            else -> Level.TRACE
        }
        // Logback level ordering: TRACE < DEBUG < INFO < WARN
        val level = if (levelNamed.isGreaterOrEqual(levelVerbose)) levelVerbose else levelNamed
        (getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = level

        getLogger(this.javaClass).debug("Hello Mailbox")
        println("Hello Mailbox")

        val mailbox = Mailbox()

        if (wipe) {
            mailbox.wipeFilesOnly()
            println("Mailbox wiped successfully \\o/")
            mailbox.getSystem().exit(0)
        }
        startLifecycle(mailbox)
    }

    private fun startLifecycle(mailbox: Mailbox) {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                mailbox.stopLifecycle(false)
                mailbox.waitForShutdown()
            }
        )

        mailbox.startLifecycle()

        if (setupToken != null) {
            try {
                mailbox.setSetupToken(setupToken!!)
            } catch (e: InvalidIdException) {
                System.err.println("Invalid setup token")
                mailbox.getSystem().exit(1)
            }
        }

        val ownerTokenExists = mailbox.getOwnerToken() != null
        if (!ownerTokenExists) {
            if (debug) {
                val token = setupToken ?: mailbox.getSetupToken()
                println(
                    "curl -v -H \"Authorization: Bearer $token\" -X PUT " +
                        "http://localhost:8000/setup"
                )
            }
            mailbox.waitForTorPublished()
            mailbox.getQrCode()?.let {
                println(QrCodeRenderer.getQrString(it))
            }
        }
    }

}

fun main(args: Array<String>) = Main().main(args)
