package org.briarproject.mailbox.android

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.mailbox.android.MailboxNotificationManager.Companion.NOTIFICATION_MAIN_ID
import org.briarproject.mailbox.core.server.WebServerManager
import javax.inject.Inject

@AndroidEntryPoint
class MailboxService : Service() {

    companion object {
        fun startService(context: Context) {
            val startIntent = Intent(context, MailboxService::class.java)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, MailboxService::class.java)
            context.stopService(stopIntent)
        }
    }

    @Inject
    internal lateinit var notificationManager: MailboxNotificationManager

    @Inject
    internal lateinit var webServerManager: WebServerManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_MAIN_ID, notificationManager.serviceNotification)
        // TODO handle inside LifecycleManager
        webServerManager.start()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        // TODO handle inside LifecycleManager
        webServerManager.stop()
        super.onDestroy()
    }
}
