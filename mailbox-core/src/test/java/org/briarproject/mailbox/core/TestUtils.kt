package org.briarproject.mailbox.core

import org.briarproject.mailbox.core.util.IoUtils
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

object TestUtils {

    private val nextTestDir = AtomicInteger(
        (Math.random() * 1000 * 1000).toInt()
    )

    fun getTestDirectory(): File {
        val name: Int = nextTestDir.getAndIncrement()
        return File("test.tmp/$name")
    }

    fun deleteTestDirectory(testDir: File) {
        IoUtils.deleteFileOrDir(testDir)
        testDir.parentFile.delete() // Delete if empty
    }

}
