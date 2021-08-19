package org.briarproject.mailbox.android

import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp
import org.briarproject.mailbox.core.AndroidEagerSingletons
import org.briarproject.mailbox.core.CoreEagerSingletons
import javax.inject.Inject

@HiltAndroidApp
class MailboxApplication : MultiDexApplication() {

    @Inject
    internal lateinit var coreEagerSingletons: CoreEagerSingletons

    @Inject
    internal lateinit var androidEagerSingletons: AndroidEagerSingletons

    override fun onCreate() {
        super.onCreate()
        MailboxService.startService(this)
    }

}
