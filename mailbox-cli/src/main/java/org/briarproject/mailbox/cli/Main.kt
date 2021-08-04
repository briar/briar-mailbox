package org.briarproject.mailbox.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.counted
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY
import java.io.File
import java.io.File.separator
import java.io.IOException
import java.lang.System.getProperty
import java.lang.System.setProperty
import java.nio.file.Files.setPosixFilePermissions
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermission.*
import java.util.logging.Level.*
import java.util.logging.LogManager

private class Main : CliktCommand(
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
    }

}

fun main(args: Array<String>) = Main().main(args)
