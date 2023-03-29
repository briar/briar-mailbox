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
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.app.ServiceCompat.stopForeground
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.android.dontkillmelib.wakelock.AndroidWakeLock
import org.briarproject.android.dontkillmelib.wakelock.AndroidWakeLockManager
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.MailboxNotificationManager.Companion.NOTIFICATION_MAIN_ID
import org.briarproject.mailbox.android.StatusManager.Starting
import org.briarproject.mailbox.android.ui.StartupFailureActivity
import org.briarproject.mailbox.android.ui.StartupFailureActivity.Companion.EXTRA_START_RESULT
import org.briarproject.mailbox.android.ui.StartupFailureActivity.StartupFailure
import org.briarproject.mailbox.android.ui.WipeCompleteActivity
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.LIFECYCLE_REUSE
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.SERVICE_ERROR
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.SUCCESS
import org.briarproject.mailbox.core.system.AndroidExecutor
import org.briarproject.mailbox.core.system.System
import org.briarproject.mailbox.core.util.LogUtils.warn
import org.slf4j.LoggerFactory.getLogger
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

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

    @Inject
    internal lateinit var androidExecutor: AndroidExecutor

    @Inject
    internal lateinit var system: System

    private lateinit var lifecycleWakeLock: AndroidWakeLock

    override fun onCreate() {
        super.onCreate()

        LOG.info("Created")
        if (created.getAndSet(true)) {
            LOG.warn("Already created")
            // This is a canary to notify us about strange behavior concerning service creation
            // in logs and bug reports. Calling stopSelf() kills the app.
            stopSelf()
            return
        }

        startForeground(
            NOTIFICATION_MAIN_ID,
            notificationManager.getServiceNotification(
                Starting(
                    status = getString(R.string.startup_headline),
                    isCancelable = false,
                )
            )
        )

        // We hold a wake lock during the whole lifecycle. We have a one-to-one relationship
        // between MailboxService and the LifecycleManager. As we do not support lifecycle restarts
        // only a single MailboxService is allowed to start and stop with the LifecycleManager
        // singleton. Should the service be killed and restarted, the LifecycleManager must also
        // have been destroyed and there is no way to recover except via a restart of the app.
        // So should a second MailboxService be started anytime, this is a unrecoverable situation
        // and we stop the app.
        // Acquiring the wakelock here and releasing it as the last thing during onDestroy()
        // makes sure it is being held during the whole lifecycle.
        lifecycleWakeLock = wakeLockManager.createWakeLock("Lifecycle")
        lifecycleWakeLock.acquire()

        // Start the services in a background thread
        androidExecutor.runOnBackgroundThread {
            val result = lifecycleManager.startServices {
                stopSelf()
                startActivity(
                    Intent(this, WipeCompleteActivity::class.java).apply {
                        flags = FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
            when (result) {
                SUCCESS -> started = true
                SERVICE_ERROR -> showStartupFailure(StartupFailure.SERVICE_ERROR)
                LIFECYCLE_REUSE -> showStartupFailure(StartupFailure.LIFECYCLE_REUSE)
            }
        }
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
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        LOG.info("Destroyed")
        stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (receiver != null) unregisterReceiver(receiver)
        if (started) {
            androidExecutor.runOnBackgroundThread {
                try {
                    lifecycleManager.stopServices(true)
                    lifecycleManager.waitForShutdown()
                } catch (e: InterruptedException) {
                    LOG.info("Interrupted while waiting for shutdown")
                } finally {
                    lifecycleWakeLock.release()
                }
            }
        }
    }

    private fun showStartupFailure(result: StartupFailure) {
        LOG.warn { "Startup failed: $result" }
        androidExecutor.runOnUiThread {
            Intent(this, StartupFailureActivity::class.java).apply {
                putExtra(EXTRA_START_RESULT, result)
                flags = FLAG_ACTIVITY_NEW_TASK
                startActivity(this)
            }
            stopSelf()
            LOG.info("Exiting")
            system.exit(1)
        }
    }
}
