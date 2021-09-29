package org.briarproject.mailbox.core.settings

import dagger.Module
import dagger.Provides
import org.briarproject.mailbox.core.db.Database

@Module
class SettingsModule {
    @Provides
    fun provideSettingsManager(db: Database): SettingsManager {
        return SettingsManagerImpl(db)
    }
}
