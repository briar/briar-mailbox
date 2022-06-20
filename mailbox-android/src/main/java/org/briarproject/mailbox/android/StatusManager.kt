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

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.briarproject.mailbox.R
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState
import org.briarproject.mailbox.core.setup.QrCodeEncoder
import org.briarproject.mailbox.core.setup.SetupComplete
import org.briarproject.mailbox.core.setup.SetupManager
import org.briarproject.mailbox.core.tor.TorPlugin
import org.briarproject.mailbox.core.tor.TorState
import javax.inject.Inject
import kotlin.math.min

class StatusManager @Inject constructor(
    @ApplicationContext private val context: Context,
    lifecycleManager: LifecycleManager,
    setupManager: SetupManager,
    private val qrCodeEncoder: QrCodeEncoder,
    torPlugin: TorPlugin,
) {

    private val lifecycleState: StateFlow<LifecycleState> =
        lifecycleManager.lifecycleStateFlow
    private val torPluginState: StateFlow<TorState> = torPlugin.state
    private val setupComplete: StateFlow<SetupComplete> = setupManager.setupComplete

    /**
     * Possible values for [appState]
     */
    sealed class MailboxAppState
    class Starting(val status: String) : MailboxAppState()
    class StartedSettingUp(val qrCode: Bitmap) : MailboxAppState()
    object StartedSetupComplete : MailboxAppState()
    object AfterRunning : MailboxAppState()
    object ErrorNoNetwork : MailboxAppState()

    val appState: Flow<MailboxAppState> = combine(
        lifecycleState, torPluginState, setupComplete
    ) { ls, ts, sc ->
        when {
            ls.isAfter(LifecycleState.RUNNING) -> AfterRunning
            ls != LifecycleState.RUNNING -> Starting(getString(R.string.startup_starting_services))
            // RUNNING
            ts != TorState.Published -> when (ts) {
                TorState.StartingStopping -> Starting(getString(R.string.startup_starting_tor))
                is TorState.Enabling -> Starting(
                    getString(R.string.startup_bootstrapping_tor, ts.percent)
                )
                TorState.Inactive -> ErrorNoNetwork
                else -> Starting(getString(R.string.startup_publishing_onion_service))
            }
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

    private fun getString(@StringRes resId: Int, vararg formatArgs: Any?): String {
        return context.getString(resId, *formatArgs)
    }

}
