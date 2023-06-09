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

package org.briarproject.mailbox.android.ui.setup

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_TEXT
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.StatusManager.MailboxAppState
import org.briarproject.mailbox.android.StatusManager.StartedSettingUp
import org.briarproject.mailbox.android.ui.BackFragment
import org.briarproject.mailbox.android.ui.MailboxViewModel
import org.briarproject.mailbox.android.ui.launchAndRepeatWhileStarted

@AndroidEntryPoint
class QrCodeLinkFragment : BackFragment() {

    private val viewModel: MailboxViewModel by activityViewModels()
    private lateinit var linkView: TextView
    private lateinit var shareButton: MaterialButton
    private lateinit var copyButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_qr_link, container, false)
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)
        linkView = v.findViewById(R.id.linkView)
        shareButton = v.findViewById(R.id.shareButton)
        copyButton = v.findViewById(R.id.copyButton)

        launchAndRepeatWhileStarted {
            viewModel.appState.collect { onAppStateChanged(it) }
        }
    }

    private fun onAppStateChanged(state: MailboxAppState) {
        if (state is StartedSettingUp) {
            linkView.text = state.link
            shareButton.setOnClickListener {
                val sendIntent = Intent().apply {
                    action = ACTION_SEND
                    putExtra(EXTRA_TEXT, state.link)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                try {
                    startActivity(shareIntent)
                } catch (ignored: ActivityNotFoundException) {
                    Toast.makeText(requireContext(), R.string.activity_not_found, LENGTH_LONG)
                        .show()
                }
            }
            copyButton.setOnClickListener {
                val clipboard = getSystemService(requireContext(), ClipboardManager::class.java)
                    ?: return@setOnClickListener
                val clip = ClipData.newPlainText("Briar Mailbox text", state.link)
                clipboard.setPrimaryClip(clip)
                // Only show a toast for Android 12 and lower.
                if (SDK_INT <= 32) Toast.makeText(context, R.string.copied, LENGTH_SHORT).show()
            }
        }
    }

}
