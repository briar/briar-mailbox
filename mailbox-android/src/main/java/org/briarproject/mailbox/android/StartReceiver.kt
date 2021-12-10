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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.content.Intent.ACTION_MY_PACKAGE_REPLACED
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.NOT_STARTED
import org.briarproject.mailbox.core.util.LogUtils.debug
import org.slf4j.LoggerFactory.getLogger
import javax.inject.Inject

private val LOG = getLogger(StartReceiver::class.java)

@AndroidEntryPoint
class StartReceiver : BroadcastReceiver() {

    @Inject
    internal lateinit var lifecycleManager: LifecycleManager

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != ACTION_BOOT_COMPLETED && action != ACTION_MY_PACKAGE_REPLACED) return

        val lifecycleState = lifecycleManager.lifecycleStateFlow.value
        LOG.debug { "Received $action in state ${lifecycleState.name}" }
        if (lifecycleState == NOT_STARTED) {
            // On API 31, we can still start a foreground service from background here:
            // https://developer.android.com/guide/components/foreground-services#background-start-restriction-exemptions
            MailboxService.startService(context)
        }
    }

}
