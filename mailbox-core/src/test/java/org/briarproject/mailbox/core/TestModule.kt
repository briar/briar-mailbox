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
import org.briarproject.mailbox.core.setup.SetupModule
import org.briarproject.mailbox.core.system.Clock
import java.io.File
import javax.inject.Singleton

@Module(
    includes = [
        LifecycleModule::class,
        TestDatabaseModule::class,
        SetupModule::class,
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
        override val root: File get() = tempDir
        override val folderRoot = File(tempDir, "folders")
        private val tempFilesDir = File(tempDir, "tmp").also { it.mkdirs() }

        override fun getTemporaryFile(fileId: String) = File(tempFilesDir, fileId).also { file ->
            // we delete root at the end of each test, so tempFilesDir gets deleted as well
            file.parentFile.mkdirs()
        }
        override fun getFolder(folderId: String) = File(folderRoot, folderId).also { it.mkdirs() }
        override fun getFile(folderId: String, fileId: String) = File(getFolder(folderId), fileId)
    }
}
