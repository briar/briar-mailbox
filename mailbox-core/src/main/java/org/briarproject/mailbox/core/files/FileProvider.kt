package org.briarproject.mailbox.core.files

import java.io.File

interface FileProvider {
    fun getTemporaryFile(fileId: String): File
    fun getFolder(folderId: String): File
    fun getFile(folderId: String, fileId: String): File
}
