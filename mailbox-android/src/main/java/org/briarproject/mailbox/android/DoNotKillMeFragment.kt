package org.briarproject.mailbox.android

import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.android.dontkillmelib.AbstractDoNotKillMeFragment

@AndroidEntryPoint
class DoNotKillMeFragment : AbstractDoNotKillMeFragment() {

    private val viewModel: MailboxViewModel by activityViewModels()

    override fun onButtonClicked() {
        viewModel.onDoNotKillComplete()
    }
}
