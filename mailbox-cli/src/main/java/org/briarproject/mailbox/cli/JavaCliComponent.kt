package org.briarproject.mailbox.cli

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        JavaCliModule::class,
    ]
)
interface JavaCliComponent {
    fun inject(main: Main)
}
