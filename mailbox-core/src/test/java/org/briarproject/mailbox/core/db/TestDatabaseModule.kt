package org.briarproject.mailbox.core.db

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.system.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class TestDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(config: DatabaseConfig, clock: Clock): Database {
        return TestH2Database(config, clock)
    }

    @Provides
    fun provideTransactionManager(db: Database): TransactionManager {
        return db
    }

}
