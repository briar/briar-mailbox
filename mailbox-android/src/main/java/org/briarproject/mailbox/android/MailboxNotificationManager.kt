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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import androidx.core.content.ContextCompat.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.StatusManager.ErrorClockSkew
import org.briarproject.mailbox.android.StatusManager.ErrorNoNetwork
import org.briarproject.mailbox.android.StatusManager.MailboxAppState
import org.briarproject.mailbox.android.StatusManager.NotStarted
import org.briarproject.mailbox.android.StatusManager.StartedSettingUp
import org.briarproject.mailbox.android.StatusManager.StartedSetupComplete
import org.briarproject.mailbox.android.StatusManager.Starting
import org.briarproject.mailbox.android.StatusManager.Stopped
import org.briarproject.mailbox.android.StatusManager.Stopping
import org.briarproject.mailbox.android.StatusManager.Wiping
import org.briarproject.mailbox.android.ui.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MailboxNotificationManager @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {

    companion object {
        private const val CHANNEL_ID = "Briar Mailbox Service"

        const val NOTIFICATION_MAIN_ID = 1
    }

    private val nm = getSystemService(ctx, NotificationManager::class.java)
        ?: error("No NotificationManager")

    init {
        if (SDK_INT >= 26) createNotificationChannels()
    }

    @RequiresApi(26)
    private fun createNotificationChannels() {
        val channels = listOf(
            NotificationChannel(
                CHANNEL_ID,
                ctx.getString(R.string.notification_channel_name),
                IMPORTANCE_LOW,
            ).apply {
                setShowBadge(false)
            }
        )
        nm.createNotificationChannels(channels)
    }

    suspend fun onMailboxAppStateChanged(state: MailboxAppState) = withContext(Dispatchers.Main) {
        val notification = getServiceNotification(state)
        nm.notify(NOTIFICATION_MAIN_ID, notification)
    }

    fun getServiceNotification(state: MailboxAppState): Notification {
        val notificationIntent = Intent(ctx, MainActivity::class.java)
        val flags = if (SDK_INT >= 23) FLAG_IMMUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, flags)
        return NotificationCompat.Builder(ctx, CHANNEL_ID).apply {
            when (state) {
                is Starting -> {
                    setContentTitle(ctx.getString(R.string.notification_mailbox_title_starting))
                    setContentText(ctx.getString(R.string.notification_mailbox_content_starting))
                }
                is StartedSettingUp -> {
                    setContentTitle(ctx.getString(R.string.notification_mailbox_title_setup))
                    setContentText(ctx.getString(R.string.notification_mailbox_content_setup))
                }
                StartedSetupComplete -> {
                    setContentTitle(ctx.getString(R.string.notification_mailbox_title_running))
                    setContentText(ctx.getString(R.string.notification_mailbox_content_running))
                }
                ErrorNoNetwork -> {
                    setContentTitle(ctx.getString(R.string.notification_mailbox_title_offline))
                    setContentText(ctx.getString(R.string.notification_mailbox_content_offline))
                }
                ErrorClockSkew -> {
                    setContentTitle(ctx.getString(R.string.notification_mailbox_title_offline))
                    setContentText(ctx.getString(R.string.notification_mailbox_content_clock_skew))
                }
                NotStarted, Stopping, Stopped, Wiping ->
                    error("No notifications when lifecycle not running")
            }
            setSmallIcon(R.drawable.ic_notification_foreground)
            setContentIntent(pendingIntent)
            priority = PRIORITY_MIN
        }.build()
    }

}
