package org.briarproject.mailbox.core.util

import org.slf4j.Logger
import java.io.File

object LogUtils {

    private const val NANOS_PER_MILLI = 1000 * 1000

    @JvmStatic
    inline fun Logger.trace(msg: () -> String) {
        if (isTraceEnabled) trace(msg())
    }

    @JvmStatic
    inline fun Logger.debug(msg: () -> String) {
        if (isDebugEnabled) debug(msg())
    }

    @JvmStatic
    inline fun Logger.info(msg: () -> String) {
        if (isInfoEnabled) info(msg())
    }

    @JvmStatic
    inline fun Logger.warn(msg: () -> String) {
        if (isWarnEnabled) warn(msg())
    }

    @JvmStatic
    inline fun Logger.error(msg: () -> String) {
        if (isErrorEnabled) error(msg())
    }

    /**
     * Returns the elapsed time in milliseconds since some arbitrary
     * starting time. This is only useful for measuring elapsed time.
     */
    @JvmStatic
    fun now(): Long {
        return System.nanoTime() / NANOS_PER_MILLI
    }

    /**
     * Logs the duration of a task.
     *
     * @param logger the logger to use
     * @param msg   a description of the task
     * @param start  the start time of the task, as returned by [now]
     */
    @JvmStatic
    fun logDuration(logger: Logger, msg: () -> String, start: Long) {
        logger.trace {
            val duration = now() - start
            "${msg()} took $duration ms"
        }
    }

    @JvmStatic
    fun logException(logger: Logger, t: Throwable) {
        if (logger.isWarnEnabled) logger.warn(t.toString(), t)
    }

    fun logFileOrDir(logger: Logger, f: File) {
        if (logger.isInfoEnabled) {
            if (f.isFile) {
                logWithType(logger, f, "F")
            } else if (f.isDirectory) {
                logWithType(logger, f, "D")
                val children = f.listFiles()
                if (children != null) {
                    for (child in children) logFileOrDir(logger, child)
                }
            } else if (f.exists()) {
                logWithType(logger, f, "?")
            }
        }
    }

    fun logWithType(logger: Logger, f: File, type: String) {
        logger.info("$type ${f.absolutePath} ${f.length()}")
    }

}
