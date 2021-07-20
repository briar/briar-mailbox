package org.briarproject.mailbox

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MailboxApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        MailboxService.startService(this)
    }

}
