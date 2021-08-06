package org.briarproject.mailbox.core

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.server.WebServerManager
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class CoreEagerSingletonsModule {

    @Provides
    @Singleton
    fun provideEagerSingletons(webServerManager: WebServerManager): CoreEagerSingletons {
        return CoreEagerSingletons()
    }

}

class CoreEagerSingletons {

    @Inject
    internal lateinit var webServerManager: WebServerManager

}