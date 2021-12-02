/*
 *     Briar Mailbox
 *     Copyright (C) 2021-2022  The Briar Project
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.briarproject.mailbox.android

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.mailbox.android.MailboxNotificationManager.Companion.NOTIFICATION_MAIN_ID
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.ALREADY_RUNNING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.SUCCESS
import org.briarproject.mailbox.core.system.AndroidWakeLockManager
import org.slf4j.LoggerFactory.getLogger
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MailboxService : Service() {

    companion object {
        private val LOG = getLogger(MailboxService::class.java)

        fun startService(context: Context) {
            val startIntent = Intent(context, MailboxService::class.java)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, MailboxService::class.java)
            context.stopService(stopIntent)
        }
    }

    private val created = AtomicBoolean(false)

    @Volatile
    internal var started = false
    private var receiver: BroadcastReceiver? = null

    @Inject
    internal lateinit var wakeLockManager: AndroidWakeLockManager

    @Inject
    internal lateinit var lifecycleManager: LifecycleManager

    @Inject
    internal lateinit var notificationManager: MailboxNotificationManager

    override fun onCreate() {
        super.onCreate()

        LOG.info("Created")
        if (created.getAndSet(true)) {
            LOG.warn("Already created")
            // FIXME when can this happen? Next line will kill app
            stopSelf()
            return
        }

        // Hold a wake lock during startup
        wakeLockManager.runWakefully({
            startForeground(NOTIFICATION_MAIN_ID, notificationManager.serviceNotification)
            // Start the services in a background thread
            wakeLockManager.executeWakefully({
                val result: StartResult = lifecycleManager.startServices()
                when {
                    result === SUCCESS -> started = true
                    result === ALREADY_RUNNING -> {
                        LOG.warn("Already running")
                        // FIXME when can this happen? Next line will kill app
                        stopSelf()
                    }
                    else -> {
                        if (LOG.isWarnEnabled) LOG.warn("Startup failed: $result")
                        // TODO: implement this
                        //  and start activity in new process, so we can kill this one
                        // showStartupFailure(result)
                        stopSelf()
                    }
                }
            }, "LifecycleStartup")
            // Register for device shutdown broadcasts
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    LOG.info("Device is shutting down")
                    stopSelf()
                }
            }
            val filter = IntentFilter()
            filter.addAction(Intent.ACTION_SHUTDOWN)
            filter.addAction("android.intent.action.QUICKBOOT_POWEROFF")
            filter.addAction("com.htc.intent.action.QUICKBOOT_POWEROFF")
            registerReceiver(receiver, filter)
        }, "LifecycleStartup")
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        wakeLockManager.runWakefully({
            super.onDestroy()
            LOG.info("Destroyed")
            stopForeground(true)
            if (receiver != null) unregisterReceiver(receiver)
            wakeLockManager.executeWakefully({
                try {
                    if (started) {
                        lifecycleManager.stopServices()
                        lifecycleManager.waitForShutdown()
                    }
                } catch (e: InterruptedException) {
                    LOG.info("Interrupted while waiting for shutdown")
                }
                LOG.info("Exiting")
                exitProcess(0)
            }, "LifecycleShutdown")
        }, "LifecycleShutdown")
    }
}
