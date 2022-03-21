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
import android.content.res.Resources
import android.graphics.Bitmap
import androidx.annotation.UiThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.briarproject.android.dontkillmelib.DozeHelper
import org.briarproject.mailbox.android.MailboxService
import org.briarproject.mailbox.android.QrCodeUtils
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState
import org.briarproject.mailbox.core.setup.QrCodeEncoder
import org.briarproject.mailbox.core.setup.SetupComplete
import org.briarproject.mailbox.core.setup.SetupManager
import org.briarproject.mailbox.core.system.DozeWatchdog
import org.briarproject.mailbox.core.tor.TorPlugin
import javax.inject.Inject
import kotlin.concurrent.thread
import kotlin.math.min

@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val app: Application,
    private val dozeHelper: DozeHelper,
    private val dozeWatchdog: DozeWatchdog,
    private val lifecycleManager: LifecycleManager,
    private val setupManager: SetupManager,
    private val qrCodeEncoder: QrCodeEncoder,
    torPlugin: TorPlugin,
) : AndroidViewModel(app) {

    val needToShowDoNotKillMeFragment get() = dozeHelper.needToShowDoNotKillMeFragment(app)

    private val _doNotKillComplete = MutableLiveData<Boolean>()
    val doNotKillComplete: LiveData<Boolean> = _doNotKillComplete

    private val lifecycleState: StateFlow<LifecycleState> = lifecycleManager.lifecycleStateFlow
    private val torPluginState: StateFlow<TorPlugin.State> = torPlugin.state

    val hasDb: LiveData<Boolean> = liveData(Dispatchers.IO) { emit(setupManager.hasDb) }

    /**
     * Possible values for [setupState]
     */
    sealed interface MailboxStartupProgress
    class Starting(val status: String) : MailboxStartupProgress
    class StartedSettingUp(val qrCode: Bitmap) : MailboxStartupProgress
    object StartedSetupComplete : MailboxStartupProgress

    val setupState = combine(
        lifecycleState, torPluginState, setupManager.setupComplete
    ) { ls, ts, sc ->
        when {
            ls != LifecycleState.RUNNING -> Starting(ls.name)
            // TODO waiting for ACTIVE is better than not doing it but to fix #90 we need to listen for
            //  upload events to the hsdirs
            ts != TorPlugin.State.ACTIVE -> Starting(ts.name + " TOR")
            sc == SetupComplete.FALSE -> {
                val dm = Resources.getSystem().displayMetrics
                val size = min(dm.widthPixels, dm.heightPixels)
                val bitMatrix = qrCodeEncoder.getQrCodeBitMatrix(size)
                StartedSettingUp(
                    bitMatrix?.let { it -> QrCodeUtils.renderQrCode(it) }
                        ?: error("The QR code bit matrix is expected to be non-null here")
                )
            }
            sc == SetupComplete.TRUE -> StartedSetupComplete
            // else means sc == SetupComplete.UNKNOWN
            else -> error("Expected setup completion to be known at this point")
        }
    }.flowOn(Dispatchers.IO)

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
