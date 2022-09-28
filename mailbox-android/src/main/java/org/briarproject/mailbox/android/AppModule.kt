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
import org.briarproject.mailbox.core.system.System
import java.io.File
import javax.inject.Singleton
import kotlin.system.exitProcess

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
    fun provideDatabaseConfig(fileProvider: FileProvider) = object : DatabaseConfig {
        override fun getDatabaseDirectory(): File {
            // The database itself does mkdirs() and we use the existence to see if DB exists
            return File(fileProvider.root, "db")
        }
    }

    @Singleton
    @Provides
    fun provideFileProvider(app: Application) = object : FileProvider {
        override val root: File get() = app.applicationContext.filesDir
        override val folderRoot = app.applicationContext.getDir("folders", MODE_PRIVATE)
        private val tempFilesDir = File(app.applicationContext.cacheDir, "tmp").apply { mkdirs() }

        override fun getTemporaryFile(fileId: String) = File(tempFilesDir, fileId)
        override fun getFolder(folderId: String) = File(folderRoot, folderId).apply { mkdirs() }
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

    @Singleton
    @Provides
    fun provideSystem() = System { code -> exitProcess(code) }
}
