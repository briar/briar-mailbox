package org.briarproject.mailbox.core.settings

import dagger.Module
import dagger.Provides
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import javax.inject.Singleton

@Module
class SettingsModule {
    @Provides
    @Singleton
    fun provideSettingsManager(db: Database): SettingsManager {
        return SettingsManagerImpl(db)
    }

    @Provides
    @Singleton
    fun provideMetadataManager(
        metadataManagerImpl: MetadataManagerImpl,
        lifecycleManager: LifecycleManager,
    ): MetadataManager {
        return metadataManagerImpl.also {
            lifecycleManager.registerOpenDatabaseHook(it)
        }
    }
}
