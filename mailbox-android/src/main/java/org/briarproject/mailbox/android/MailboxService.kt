package org.briarproject.mailbox.android

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.mailbox.android.MailboxNotificationManager.Companion.NOTIFICATION_MAIN_ID
import org.briarproject.mailbox.android.api.system.AndroidWakeLockManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.ALREADY_RUNNING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.SUCCESS
import java.util.logging.Level
import java.util.logging.Logger
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

    private val LOG = Logger.getLogger(MailboxService::class.java.name)

    @Volatile
    internal var started = false

    @Inject
    internal lateinit var wakeLockManager: AndroidWakeLockManager

    @Inject
    internal lateinit var lifecycleManager: LifecycleManager

    @Inject
    internal lateinit var notificationManager: MailboxNotificationManager

    override fun onCreate() {
        super.onCreate()

        // Hold a wake lock during startup
        wakeLockManager.runWakefully({
            startForeground(NOTIFICATION_MAIN_ID, notificationManager.serviceNotification)
            // Start the services in a background thread
            wakeLockManager.executeWakefully({
                val result: StartResult = lifecycleManager.startServices()
                if (result === SUCCESS) {
                    started = true
                } else if (result === ALREADY_RUNNING) {
                    LOG.info("Already running")
                    stopSelf()
                } else {
                    if (LOG.isLoggable(Level.WARNING))
                        LOG.warning("Startup failed: $result")
                    // TODO: implement this
                    // showStartupFailure(result)
                    stopSelf()
                }
            }, "LifecycleStartup")
        }, "LifecycleStartup")
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        wakeLockManager.runWakefully({
            super.onDestroy()
            wakeLockManager.executeWakefully(
                { lifecycleManager.stopServices() },
                "LifecycleShutdown"
            )
        }, "LifecycleShutdown")
    }
}
