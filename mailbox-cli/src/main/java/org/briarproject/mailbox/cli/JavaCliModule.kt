package org.briarproject.mailbox.cli

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.CoreModule
import org.briarproject.mailbox.core.db.DatabaseConfig
import org.briarproject.mailbox.core.event.DefaultEventExecutorModule
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

    @Singleton
    @Provides
    fun provideDatabaseConfig() = object : DatabaseConfig {
        override fun getDatabaseDirectory(): File {
            val dataDir = getDataDir()
            val dbDir = File(dataDir, "db")
            if (!dbDir.exists() && !dbDir.mkdirs()) {
                throw IOException("dbDir could not be created: ${dbDir.absolutePath}")
            } else if (!dbDir.isDirectory) {
                throw IOException("dbDir is not a directory: ${dbDir.absolutePath}")
            }
            return dbDir
        }
    }

    private fun getDataDir(): File {
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
        return dataDir
    }

}
