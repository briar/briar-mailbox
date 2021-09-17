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
import java.io.File
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
    @Singleton
    @Provides
    fun provideDatabaseConfig() = object : DatabaseConfig {
        override fun getDatabaseDirectory(): File {
            // TODO: use a correct location
            return File("/tmp")
        }
    }
}
