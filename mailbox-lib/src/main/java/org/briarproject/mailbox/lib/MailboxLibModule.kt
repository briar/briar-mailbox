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

package org.briarproject.mailbox.lib

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.CoreModule
import org.briarproject.mailbox.core.db.DatabaseConfig
import org.briarproject.mailbox.core.event.DefaultEventExecutorModule
import org.briarproject.mailbox.core.files.FileProvider
import org.briarproject.mailbox.core.system.DefaultTaskSchedulerModule
import org.briarproject.mailbox.core.tor.JavaTorModule
import org.briarproject.mailbox.core.util.LogUtils.info
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.io.File.separator
import java.io.IOException
import java.nio.file.Files.setPosixFilePermissions
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
import javax.inject.Singleton

@Module(
    includes = [
        CoreModule::class,
        DefaultEventExecutorModule::class,
        DefaultTaskSchedulerModule::class,
        JavaTorModule::class,
    ]
)
@InstallIn(SingletonComponent::class)
open class MailboxLibModule(private val customDataDir: File? = null) {

    companion object {
        private val LOG: Logger = getLogger(MailboxLibModule::class.java)

        private val DEFAULT_DATAHOME = System.getProperty("user.home") +
            separator + ".local" + separator + "share"
        private const val DATAHOME_SUBDIR = "briar-mailbox"
    }

    /**
     * Returns the [File] for the data directory of the mailbox.
     * If XDG_DATA_HOME is defined, it returns "$XDG_DATA_HOME/briar-mailbox"
     * and otherwise it returns "~/.local/share/briar-mailbox".
     */
    private val dataDir: File by lazy {
        val dataDir = if (customDataDir != null) {
            customDataDir
        } else {
            val dataHome = when (val custom = System.getenv("XDG_DATA_HOME").orEmpty()) {
                "" -> File(DEFAULT_DATAHOME)
                else -> File(custom)
            }
            if (!dataHome.exists() || !dataHome.isDirectory) {
                throw IOException("datahome missing or not a directory: ${dataHome.absolutePath}")
            }
            File(dataHome.absolutePath + separator + DATAHOME_SUBDIR)
        }

        if (!dataDir.exists() && !dataDir.mkdirs()) {
            throw IOException("datadir could not be created: ${dataDir.absolutePath}")
        } else if (!dataDir.isDirectory) {
            throw IOException("datadir is not a directory: ${dataDir.absolutePath}")
        }

        val perms = HashSet<PosixFilePermission>()
        perms.add(OWNER_READ)
        perms.add(OWNER_WRITE)
        perms.add(OWNER_EXECUTE)
        setPosixFilePermissions(dataDir.toPath(), perms)

        LOG.info { "Datadir set to: ${dataDir.absolutePath}" }
        dataDir
    }

    @Singleton
    @Provides
    fun provideDatabaseConfig(fileProvider: FileProvider) = object : DatabaseConfig {
        override fun getDatabaseDirectory(): File {
            // The database itself does mkdirs() and we use the existence to see if DB exists
            return File(fileProvider.root, "db")
        }
    }

    @Singleton
    @Provides
    fun provideFileProvider() = object : FileProvider {
        override val root: File get() = dataDir
        private val tempFilesDir = File(dataDir, "tmp").apply { mkdirs() }
        override val folderRoot = File(dataDir, "folders").apply { mkdirs() }

        override fun getTemporaryFile(fileId: String) = File(tempFilesDir, fileId)
        override fun getFolder(folderId: String) = File(folderRoot, folderId).apply { mkdirs() }
        override fun getFile(folderId: String, fileId: String) = File(getFolder(folderId), fileId)
    }

}
