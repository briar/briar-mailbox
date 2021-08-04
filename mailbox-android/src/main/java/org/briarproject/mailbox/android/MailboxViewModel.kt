package org.briarproject.mailbox.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(
    app: Application,
    handle: SavedStateHandle,
) : AndroidViewModel(app) {

    private val _text = handle.getLiveData("text", "Hello Mailbox")
    val text: LiveData<String> = _text

    fun updateText(str: String) {
        _text.value = str
    }

}
