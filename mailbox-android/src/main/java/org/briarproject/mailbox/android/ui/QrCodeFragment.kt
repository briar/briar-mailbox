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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.StatusManager.MailboxAppState
import org.briarproject.mailbox.android.StatusManager.StartedSettingUp

@AndroidEntryPoint
class QrCodeFragment : Fragment(), MenuProvider {

    private val viewModel: MailboxViewModel by activityViewModels()
    private lateinit var qrCodeView: ImageView
    private lateinit var buttonCancel: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_qr, container, false)
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        qrCodeView = v.findViewById(R.id.qrcode)
        buttonCancel = v.findViewById(R.id.buttonCancel)

        buttonCancel.setOnClickListener {
            viewModel.stopLifecycle()
            requireActivity().finishAffinity()
        }
        requireActivity().addMenuProvider(this, viewLifecycleOwner, RESUMED)

        launchAndRepeatWhileStarted {
            viewModel.appState.collect { onAppStateChanged(it) }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.link_actions, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_show_link) {
            findNavController().navigate(R.id.action_qrCodeFragment_to_qrCodeLinkFragment)
            return true
        }
        return false
    }

    private fun onAppStateChanged(state: MailboxAppState) {
        if (state is StartedSettingUp) {
            qrCodeView.setImageBitmap(state.qrCode)
        }
    }

}
