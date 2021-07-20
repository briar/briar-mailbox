package org.briarproject.mailbox

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.mailbox.MailboxNotificationManager.Companion.NOTIFICATION_MAIN_ID
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
    lateinit var notificationManager: MailboxNotificationManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_MAIN_ID, notificationManager.serviceNotification)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

}
