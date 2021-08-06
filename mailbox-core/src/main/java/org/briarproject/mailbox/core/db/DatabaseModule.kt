package org.briarproject.mailbox.core.db

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabaseComponent(): DatabaseComponent {
        return DatabaseComponentImpl()
    }

}
