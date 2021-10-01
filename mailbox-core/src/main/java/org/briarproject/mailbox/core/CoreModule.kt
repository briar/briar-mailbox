package org.briarproject.mailbox.core

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.db.DatabaseModule
import org.briarproject.mailbox.core.event.EventModule
import org.briarproject.mailbox.core.lifecycle.LifecycleModule
import org.briarproject.mailbox.core.server.WebServerModule
import org.briarproject.mailbox.core.settings.SettingsModule
import org.briarproject.mailbox.core.system.Clock
import org.briarproject.mailbox.core.tor.TorModule
import javax.inject.Singleton

@Module(
    includes = [
        EventModule::class,
        LifecycleModule::class,
        DatabaseModule::class,
        WebServerModule::class,
        SettingsModule::class,
        TorModule::class,
    ]
)
@InstallIn(SingletonComponent::class)
class CoreModule {
    @Singleton
    @Provides
    fun provideClock() = Clock { System.currentTimeMillis() }
}
