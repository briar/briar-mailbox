package org.briarproject.mailbox.core.lifecycle

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class LifecycleModule {

    @Provides
    @Singleton
    fun provideLifecycleManager(lifecycleManager: LifecycleManagerImpl): LifecycleManager {
        return lifecycleManager
    }

}
