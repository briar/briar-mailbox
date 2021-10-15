package org.briarproject.mailbox.core

import dagger.Component
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.settings.SettingsManager
import org.briarproject.mailbox.core.system.RandomIdManager
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
    fun getDatabase(): Database
    fun getRandomIdManager(): RandomIdManager
}
