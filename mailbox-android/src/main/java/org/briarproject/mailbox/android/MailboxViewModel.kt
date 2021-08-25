package org.briarproject.mailbox.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(
    app: Application,
    handle: SavedStateHandle,
    lifecycleManager: LifecycleManager,
) : AndroidViewModel(app) {

    private val _text = handle.getLiveData("text", "Hello Mailbox")
    val text: LiveData<String> = _text

    val lifecycleState: StateFlow<LifecycleState> = lifecycleManager.lifecycleStateFlow

    fun startLifecycle() {
        MailboxService.startService(getApplication())
    }

    fun stopLifecycle() {
        MailboxService.stopService(getApplication())
    }

    fun updateText(str: String) {
        _text.value = str
    }

}
