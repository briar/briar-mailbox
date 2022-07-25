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

package org.briarproject.mailbox.android.ui

import android.app.Application
import androidx.annotation.UiThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.briarproject.android.dontkillmelib.DozeHelper
import org.briarproject.mailbox.android.MailboxPreferences
import org.briarproject.mailbox.android.MailboxService
import org.briarproject.mailbox.android.StatusManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState
import org.briarproject.mailbox.core.settings.MetadataManager
import org.briarproject.mailbox.core.setup.SetupManager
import org.briarproject.mailbox.core.system.AndroidExecutor
import org.briarproject.mailbox.core.system.DozeWatchdog
import org.briarproject.mailbox.core.util.LogUtils.info
import org.slf4j.LoggerFactory.getLogger
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val app: Application,
    private val dozeHelper: DozeHelper,
    private val dozeWatchdog: DozeWatchdog,
    private val lifecycleManager: LifecycleManager,
    private val setupManager: SetupManager,
    statusManager: StatusManager,
    metadataManager: MetadataManager,
    private val mailboxPreferences: MailboxPreferences,
    private val androidExecutor: AndroidExecutor,
) : AndroidViewModel(app) {

    companion object {
        private val LOG = getLogger(MailboxViewModel::class.java)
    }

    init {
        LOG.info("Created MailboxViewModel")
    }

    val needToShowDoNotKillMeFragment get() = dozeHelper.needToShowDoNotKillMeFragment(app)

    private val _doNotKillComplete = MutableLiveData<Boolean>()
    val doNotKillComplete: LiveData<Boolean> = _doNotKillComplete

    val lifecycleState: StateFlow<LifecycleState> = lifecycleManager.lifecycleStateFlow

    suspend fun hasDb(): Boolean = withContext(Dispatchers.IO) {
        setupManager.hasDb
    }

    val appState = statusManager.appState

    val lastAccess: LiveData<Long> = metadataManager.ownerConnectionTime.asLiveData()

    @UiThread
    fun onDoNotKillComplete() {
        _doNotKillComplete.value = true
    }

    override fun onCleared() {
        LOG.info { "cleared" }
    }

    /**
     * Starts the mailbox lifecycle and sets an internal flag in order to autostart the mailbox on
     * subsequent boots of the device.
     */
    fun startLifecycle() {
        setAutoStartEnabled(true)
        MailboxService.startService(getApplication())
    }

    /**
     * Stops the mailbox lifecycle and unsets an internal flag in order to not autostart the mailbox
     * on subsequent boots of the device.
     */
    fun stopLifecycle() {
        setAutoStartEnabled(false)
        MailboxService.stopService(getApplication())
    }

    fun wipe() {
        androidExecutor.runOnBackgroundThread {
            LOG.info("calling wipeMailbox()")
            // TODO: handle return value
            lifecycleManager.wipeMailbox()
        }
    }

    fun getAndResetDozeFlag() = dozeWatchdog.andResetDozeFlag

    private fun setAutoStartEnabled(enabled: Boolean) {
        mailboxPreferences.setAutoStartEnabled(enabled)
    }

}
