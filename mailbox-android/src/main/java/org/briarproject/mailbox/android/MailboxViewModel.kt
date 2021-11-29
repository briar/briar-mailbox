package org.briarproject.mailbox.android

import android.app.Application
import androidx.annotation.UiThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import org.briarproject.android.dontkillmelib.DozeHelper
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState
import org.briarproject.mailbox.core.system.AndroidWakeLockManager
import org.briarproject.mailbox.core.system.DozeWatchdog
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val app: Application,
    private val dozeHelper: DozeHelper,
    private val dozeWatchdog: DozeWatchdog,
    handle: SavedStateHandle,
    private val lifecycleManager: LifecycleManager,
    private val wakeLockManager: AndroidWakeLockManager,
) : AndroidViewModel(app) {

    val needToShowDoNotKillMeFragment get() = dozeHelper.needToShowDoNotKillMeFragment(app)

    private val _doNotKillComplete = MutableLiveData<Boolean>()
    val doNotKillComplete: LiveData<Boolean> = _doNotKillComplete

    private val _text = handle.getLiveData("text", "Hello Mailbox")
    val text: LiveData<String> = _text

    val lifecycleState: StateFlow<LifecycleState> = lifecycleManager.lifecycleStateFlow

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
        wakeLockManager.executeWakefully({
            lifecycleManager.wipeMailbox()
            MailboxService.stopService(getApplication())
        }, "LifecycleWipe")
    }

    fun getAndResetDozeFlag() = dozeWatchdog.andResetDozeFlag

    fun updateText(str: String) {
        _text.value = str
    }

}
