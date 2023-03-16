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

package org.briarproject.mailbox.android.ui.status

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.UiUtils.formatDate
import org.briarproject.mailbox.android.ui.MailboxViewModel
import org.briarproject.mailbox.android.ui.launchAndRepeatWhileStarted

@AndroidEntryPoint
class StatusFragment : Fragment() {

    private val viewModel: MailboxViewModel by activityViewModels()

    private lateinit var buttonStop: Button
    private lateinit var buttonHelpStop: ImageButton
    private lateinit var buttonUnlink: Button
    private lateinit var textViewDescription: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_status, container, false)
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        buttonStop = v.findViewById(R.id.buttonStop)
        buttonHelpStop = v.findViewById(R.id.buttonHelpStop)
        buttonUnlink = v.findViewById(R.id.buttonUnlink)
        textViewDescription = v.findViewById(R.id.description)

        buttonStop.setOnClickListener {
            MaterialAlertDialogBuilder(
                requireContext(), R.style.Theme_BriarMailbox_Dialog_Destructive
            ).setTitle(R.string.confirm_stop_title)
                .setMessage(R.string.confirm_stop_description)
                .setPositiveButton(R.string.stop) { _, _ -> viewModel.stopLifecycle() }
                .setNegativeButton(R.string.cancel, null)
                .create().show()
        }
        buttonHelpStop.setOnClickListener {
            MaterialAlertDialogBuilder(
                requireContext(), R.style.Theme_BriarMailbox_Dialog
            ).setMessage(R.string.confirm_stop_description)
                .setNeutralButton(R.string.ok, null)
                .create().show()
        }
        buttonUnlink.setOnClickListener {
            MaterialAlertDialogBuilder(
                requireContext(), R.style.Theme_BriarMailbox_Dialog_Destructive
            ).setTitle(R.string.unlink_title)
                .setMessage(R.string.unlink_description)
                .setPositiveButton(R.string.unlink) { _, _ -> viewModel.wipe() }
                .setNegativeButton(R.string.cancel, null)
                .create().show()
        }

        viewModel.lastAccess.observe(viewLifecycleOwner) { onLastAccessChanged(it) }
        launchAndRepeatWhileStarted {
            viewModel.appState.collect { onAppStateChanged() }
        }
    }

    private fun onLastAccessChanged(lastAccess: Long) {
        textViewDescription.text =
            getString(R.string.last_connection, formatDate(requireContext(), lastAccess))
    }

    private fun onAppStateChanged() {
        onLastAccessChanged(viewModel.lastAccess.value ?: 0)
    }

}
