package org.briarproject.mailbox.core

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.db.DatabaseConfig
import org.briarproject.mailbox.core.db.DatabaseModule
import org.briarproject.mailbox.core.lifecycle.LifecycleModule
import org.briarproject.mailbox.core.server.WebServerModule
import org.briarproject.mailbox.core.system.Clock
import java.io.File
import javax.inject.Singleton

@Module(
    includes = [
        LifecycleModule::class,
        DatabaseModule::class,
        WebServerModule::class,
        // no Tor module
    ]
)
@InstallIn(SingletonComponent::class)
internal class TestModule {
    @Singleton
    @Provides
    fun provideClock() = Clock { System.currentTimeMillis() }

    @Singleton
    @Provides
    fun provideDatabaseConfig() = object : DatabaseConfig {
        override fun getDatabaseDirectory(): File {
            return File("/tmp")
        }
    }
}
