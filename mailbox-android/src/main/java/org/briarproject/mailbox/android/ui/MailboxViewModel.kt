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
import androidx.lifecycle.liveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.briarproject.android.dontkillmelib.DozeHelper
import org.briarproject.mailbox.android.MailboxService
import org.briarproject.mailbox.android.StatusManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.setup.SetupManager
import org.briarproject.mailbox.core.system.DozeWatchdog
import javax.inject.Inject
import kotlin.concurrent.thread

@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val app: Application,
    private val dozeHelper: DozeHelper,
    private val dozeWatchdog: DozeWatchdog,
    private val lifecycleManager: LifecycleManager,
    private val setupManager: SetupManager,
    statusManager: StatusManager,
) : AndroidViewModel(app) {

    val needToShowDoNotKillMeFragment get() = dozeHelper.needToShowDoNotKillMeFragment(app)

    private val _doNotKillComplete = MutableLiveData<Boolean>()
    val doNotKillComplete: LiveData<Boolean> = _doNotKillComplete

    val hasDb: LiveData<Boolean> = liveData(Dispatchers.IO) { emit(setupManager.hasDb) }

    val setupState = statusManager.setupState

    @UiThread
    fun onDoNotKillComplete() {
        _doNotKillComplete.value = true
    }

    fun startLifecycle() {
        MailboxService.startService(getApplication())
    }

    fun stopLifecycle() {
        MailboxService.stopService(getApplication())
    }

    fun wipe() {
        thread {
            // TODO: handle return value
            lifecycleManager.wipeMailbox()
            MailboxService.stopService(getApplication())
        }
    }

    fun getAndResetDozeFlag() = dozeWatchdog.andResetDozeFlag

}
