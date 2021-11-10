package org.briarproject.mailbox.core

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.db.DatabaseConfig
import org.briarproject.mailbox.core.db.TestDatabaseModule
import org.briarproject.mailbox.core.files.FileProvider
import org.briarproject.mailbox.core.lifecycle.LifecycleModule
import org.briarproject.mailbox.core.server.WebServerModule
import org.briarproject.mailbox.core.settings.SettingsModule
import org.briarproject.mailbox.core.system.Clock
import java.io.File
import javax.inject.Singleton

@Module(
    includes = [
        LifecycleModule::class,
        TestDatabaseModule::class,
        WebServerModule::class,
        SettingsModule::class,
        // no Tor module
    ]
)
@InstallIn(SingletonComponent::class)
internal class TestModule(private val tempDir: File) {
    @Singleton
    @Provides
    fun provideClock() = Clock { System.currentTimeMillis() }

    @Singleton
    @Provides
    fun provideDatabaseConfig() = object : DatabaseConfig {
        override fun getDatabaseDirectory(): File {
            return File(tempDir, "db")
        }
    }

    @Singleton
    @Provides
    fun provideFileProvider() = object : FileProvider {
        private val tempFilesDir = File(tempDir, "tmp").also { it.mkdirs() }
        override val folderRoot = File(tempDir, "folders")

        override fun getTemporaryFile(fileId: String) = File(tempFilesDir, fileId)
        override fun getFolder(folderId: String) = File(folderRoot, folderId).also { it.mkdirs() }
        override fun getFile(folderId: String, fileId: String) = File(getFolder(folderId), fileId)
    }
}
