package org.briarproject.mailbox.system

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.system.System
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class ProductionSystemModule {

    @Singleton
    @Provides
    fun provideSystem(): System = ProductionSystem()

}
