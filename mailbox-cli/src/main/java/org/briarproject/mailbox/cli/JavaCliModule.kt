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
internal class JavaCliModule {

    companion object {
        private val LOG: Logger = getLogger(JavaCliModule::class.java)

        private val DEFAULT_DATAHOME = System.getProperty("user.home") +
            separator + ".local" + separator + "share"
        private const val DATAHOME_SUBDIR = "briar-mailbox"
    }

    private val dataDir: File by lazy {
        val dataHome = when (val custom = System.getenv("XDG_DATA_HOME").orEmpty()) {
            "" -> File(DEFAULT_DATAHOME)
            else -> File(custom)
        }
        if (!dataHome.exists() || !dataHome.isDirectory) {
            throw IOException("datahome missing or not a directory: ${dataHome.absolutePath}")
        }

        val dataDir = File(dataHome.absolutePath + separator + DATAHOME_SUBDIR)
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
    fun provideDatabaseConfig() = object : DatabaseConfig {
        override fun getDatabaseDirectory(): File {
            val dbDir = File(dataDir, "db")
            if (!dbDir.exists() && !dbDir.mkdirs()) {
                throw IOException("dbDir could not be created: ${dbDir.absolutePath}")
            } else if (!dbDir.isDirectory) {
                throw IOException("dbDir is not a directory: ${dbDir.absolutePath}")
            }
            return dbDir
        }
    }

    @Singleton
    @Provides
    fun provideFileProvider() = object : FileProvider {
        private val tempFilesDir = File(dataDir, "tmp").also { it.mkdirs() }
        override val folderRoot = File(dataDir, "folders").also { it.mkdirs() }

        override fun getTemporaryFile(fileId: String) = File(tempFilesDir, fileId)
        override fun getFolder(folderId: String) = File(folderRoot, folderId).also { it.mkdirs() }
        override fun getFile(folderId: String, fileId: String) = File(getFolder(folderId), fileId)
    }

}
