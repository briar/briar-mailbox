package org.briarproject.mailbox.android

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.CoreModule
import org.briarproject.mailbox.core.db.DatabaseConfig
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
            return app.applicationContext.getDir("db", Context.MODE_PRIVATE)
        }
    }
}
