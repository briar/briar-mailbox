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
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.mailbox.R
import org.briarproject.mailbox.core.system.AndroidExecutor
import org.briarproject.mailbox.core.system.System
import org.slf4j.LoggerFactory.getLogger
import javax.inject.Inject

@AndroidEntryPoint
class WipeCompleteFragment : Fragment() {

    companion object {
        private val LOG = getLogger(WipeCompleteFragment::class.java)
    }

    private val viewModel: MailboxViewModel by activityViewModels()

    @Inject
    internal lateinit var system: System

    @Inject
    internal lateinit var androidExecutor: AndroidExecutor

    private lateinit var description: View
    private lateinit var button: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_wipe_complete, container, false)
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        description = v.findViewById(R.id.description)
        androidExecutor.runOnBackgroundThread {
            if (viewModel.hasBeenWipedLocally()) {
                androidExecutor.runOnUiThread {
                    description.visibility = VISIBLE
                }
            }
        }

        button = v.findViewById(R.id.button)

        button.setOnClickListener {
            LOG.info("Exiting")
            system.exit(0)
        }
    }

}
