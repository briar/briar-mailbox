package org.briarproject.mailbox.android

import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MailboxApplication : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        MailboxService.startService(this)
    }

}
