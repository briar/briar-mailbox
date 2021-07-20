package org.briarproject.mailbox

import dagger.Component

@Component
interface ApplicationComponent {

    fun inject(activity: MainActivity)

}