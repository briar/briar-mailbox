package org.briarproject.mailbox.core

import dagger.Component
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.db.DatabaseConfig
import org.briarproject.mailbox.core.files.FileManager
import org.briarproject.mailbox.core.files.FileProvider
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.server.WebServerManager
import org.briarproject.mailbox.core.settings.MetadataManager
import org.briarproject.mailbox.core.settings.SettingsManager
import org.briarproject.mailbox.core.setup.SetupManager
import org.briarproject.mailbox.core.setup.WipeManager
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        TestModule::class,
    ]
)
interface TestComponent {
    fun injectCoreEagerSingletons(): CoreEagerSingletons
    fun getLifecycleManager(): LifecycleManager
    fun getSettingsManager(): SettingsManager
    fun getSetupManager(): SetupManager
    fun getFileManager(): FileManager
    fun getDatabaseConfig(): DatabaseConfig
    fun getDatabase(): Database
    fun getFileProvider(): FileProvider
    fun getMetadataManager(): MetadataManager
    fun getWebServerManager(): WebServerManager
    fun getWipeManager(): WipeManager
}
