package org.briarproject.mailbox.android

import android.app.Application
import android.content.Context.MODE_PRIVATE
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.android.dontkillmelib.DozeHelper
import org.briarproject.android.dontkillmelib.DozeHelperImpl
import org.briarproject.mailbox.core.CoreModule
import org.briarproject.mailbox.core.db.DatabaseConfig
import org.briarproject.mailbox.core.files.FileProvider
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.system.DozeWatchdog
import java.io.File
import javax.inject.Singleton

@Module(
    includes = [
        CoreModule::class,
        // Hilt modules from this gradle module are included automatically somehow
    ]
)
@InstallIn(SingletonComponent::class)
internal class AppModule {
    @Singleton
    @Provides
    fun provideDatabaseConfig(app: Application) = object : DatabaseConfig {
        override fun getDatabaseDirectory(): File {
            return app.applicationContext.getDir("db", MODE_PRIVATE)
        }
    }

    @Singleton
    @Provides
    fun provideFileProvider(app: Application) = object : FileProvider {
        private val tempFilesDir = File(app.applicationContext.cacheDir, "tmp").also { it.mkdirs() }
        override val folderRoot = app.applicationContext.getDir("folders", MODE_PRIVATE)

        override fun getTemporaryFile(fileId: String) = File(tempFilesDir, fileId)
        override fun getFolder(folderId: String) = File(folderRoot, folderId).also { it.mkdirs() }
        override fun getFile(folderId: String, fileId: String) = File(getFolder(folderId), fileId)
    }

    @Singleton
    @Provides
    fun provideDozeWatchdog(app: Application, lifecycleManager: LifecycleManager): DozeWatchdog {
        return DozeWatchdog(app).also { lifecycleManager.registerService(it) }
    }

    @Singleton
    @Provides
    fun provideDozeHelper(): DozeHelper = DozeHelperImpl()
}
