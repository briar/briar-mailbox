package org.briarproject.mailbox.android

import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp
import org.briarproject.mailbox.core.CoreEagerSingletons
import javax.inject.Inject

@HiltAndroidApp
class MailboxApplication : MultiDexApplication() {

    @Inject
    lateinit var coreEagerSingletons: CoreEagerSingletons

    override fun onCreate() {
        super.onCreate()
        MailboxService.startService(this)
    }

}
