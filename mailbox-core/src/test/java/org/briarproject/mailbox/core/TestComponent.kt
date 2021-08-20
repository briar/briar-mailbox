package org.briarproject.mailbox.core

import dagger.Component
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
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
}
