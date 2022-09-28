package org.briarproject.mailbox.system

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.system.System
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class TestSystemModule {

    @Singleton
    @Provides
    fun provideSystem(system: TestSystem): System = system

    @Singleton
    @Provides
    fun provideTestSystem(): TestSystem = TestSystem()

}
