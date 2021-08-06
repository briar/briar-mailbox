package org.briarproject.mailbox.core

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.db.DatabaseModule
import org.briarproject.mailbox.core.lifecycle.LifecycleModule
import org.briarproject.mailbox.core.server.WebServerModule

@Module(
    includes = [
        CoreEagerSingletonsModule::class,
        LifecycleModule::class,
        DatabaseModule::class,
        WebServerModule::class,
    ]
)
@InstallIn(SingletonComponent::class)
class CoreModule