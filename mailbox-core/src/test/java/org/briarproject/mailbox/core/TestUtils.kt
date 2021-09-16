package org.briarproject.mailbox.core

import org.briarproject.mailbox.core.util.IoUtils
import java.io.File

object TestUtils {

    fun deleteTestDirectory(testDir: File) {
        IoUtils.deleteFileOrDir(testDir)
        testDir.parentFile.delete() // Delete if empty
    }

}
