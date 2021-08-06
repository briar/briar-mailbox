package org.briarproject.mailbox.core.server

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class WebServerModule {

    @Provides
    @Singleton
    fun provideWebServer(lifecycleManager: LifecycleManager): WebServerManager {
        val webServerManager = WebServerManager()
        lifecycleManager.registerService(webServerManager)
        return webServerManager
    }

}
