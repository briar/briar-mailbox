package org.briarproject.mailbox.android

import dagger.Component

@Component
interface ApplicationComponent {

    fun inject(activity: MainActivity)

}
