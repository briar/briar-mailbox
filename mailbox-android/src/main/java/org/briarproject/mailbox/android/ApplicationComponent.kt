package org.briarproject.mailbox.android

import dagger.Component

@Component(
    modules = [
        AppModule::class,
    ]
)
interface ApplicationComponent {

    fun inject(activity: MainActivity)

}
