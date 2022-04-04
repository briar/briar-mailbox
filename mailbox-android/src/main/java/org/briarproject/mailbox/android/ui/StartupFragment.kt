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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.ui.MailboxViewModel.MailboxStartupProgress
import org.briarproject.mailbox.android.ui.MailboxViewModel.StartedSettingUp
import org.briarproject.mailbox.android.ui.MailboxViewModel.Starting
import org.briarproject.mailbox.android.ui.StartupFragmentDirections.actionStartupFragmentToQrCodeFragment
import org.briarproject.mailbox.android.ui.StartupFragmentDirections.actionStartupFragmentToStatusFragment

@AndroidEntryPoint
class StartupFragment : Fragment() {

    private val viewModel: MailboxViewModel by activityViewModels()
    private lateinit var statusTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_startup, container, false)
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        statusTextView = v.findViewById(R.id.statusTextView)

        // Start a coroutine in the lifecycle scope
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Trigger the flow and start listening for values.
                // Note that this happens when lifecycle is STARTED and stops
                // collecting when the lifecycle is STOPPED
                viewModel.setupState.collect { onSetupStateChanged(it) }
            }
        }

        viewModel.startLifecycle()
    }

    private fun onSetupStateChanged(state: MailboxStartupProgress) {
        when (state) {
            is Starting -> statusTextView.text = state.status
            is StartedSettingUp -> findNavController().navigate(
                actionStartupFragmentToQrCodeFragment()
            )
            is MailboxViewModel.StartedSetupComplete -> findNavController().navigate(
                actionStartupFragmentToStatusFragment()
            )
        }
    }

}