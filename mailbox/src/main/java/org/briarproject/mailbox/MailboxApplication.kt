package org.briarproject.mailbox

import android.app.Application

class MailboxApplication : Application() {

    val appComponent = DaggerApplicationComponent.create()

}