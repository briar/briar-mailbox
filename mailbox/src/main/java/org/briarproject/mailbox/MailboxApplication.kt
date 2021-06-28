package org.briarproject.mailbox

import android.app.Application

class MailboxApplication : Application() {

    val appComponent = DaggerApplicationComponent.create()

    override fun onCreate() {
        super.onCreate()
        MailboxService.startService(this, "Waiting for messages")
    }

}