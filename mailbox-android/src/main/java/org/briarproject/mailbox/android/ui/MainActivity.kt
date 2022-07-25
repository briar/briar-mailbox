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

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.briarproject.android.dontkillmelib.DozeUtils.needsDozeWhitelisting
import org.briarproject.mailbox.NavOnboardingDirections.actionGlobalStoppingFragment
import org.briarproject.mailbox.NavOnboardingDirections.actionGlobalWipingFragment
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.dontkillme.DoNotKillMeFragmentDirections.actionDoNotKillMeFragmentToStartupFragment
import org.briarproject.mailbox.android.ui.InitFragmentDirections.actionInitFragmentToDoNotKillMeFragment
import org.briarproject.mailbox.android.ui.InitFragmentDirections.actionInitFragmentToStartupFragment
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STOPPING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.WIPING
import org.briarproject.mailbox.core.util.LogUtils.info
import org.slf4j.LoggerFactory.getLogger

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ActivityResultCallback<ActivityResult> {

    companion object {
        private val LOG = getLogger(MainActivity::class.java)
    }

    private val viewModel: MailboxViewModel by viewModels()
    private val nav: NavController by lazy {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
        navHostFragment.navController
    }

    private val startForResult = registerForActivityResult(StartActivityForResult(), this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LOG.info("onCreate()")
        setContentView(R.layout.activity_main)

        viewModel.doNotKillComplete.observe(this) { complete ->
            if (complete && nav.currentDestination?.id == R.id.doNotKillMeFragment) nav.navigate(
                actionDoNotKillMeFragmentToStartupFragment()
            )
        }

        launchAndRepeatWhileStarted {
            viewModel.lifecycleState.collect { state ->
                LOG.info { "lifecycle state: $state" }
                when (state) {
                    STOPPING -> nav.navigate(actionGlobalStoppingFragment())
                    WIPING -> nav.navigate(actionGlobalWipingFragment())
                    else -> {}
                }
            }
        }

        LOG.info { "do we have a saved instance state? " + (savedInstanceState != null) }

        lifecycleScope.launch {
            val hasDb = viewModel.hasDb()
            LOG.info { "do we have a db? $hasDb" }
            onDbChecked(hasDb, savedInstanceState)
        }
    }

    private fun onDbChecked(hasDb: Boolean, savedInstanceState: Bundle?) {
        if (lifecycle.currentState == DESTROYED) {
            return
        }
        if (savedInstanceState == null) {
            if (!hasDb) {
                startForResult.launch(Intent(this, OnboardingActivity::class.java))
            } else if (needsDozeWhitelisting(this)) {
                nav.navigate(actionInitFragmentToDoNotKillMeFragment())
            } else {
                nav.navigate(actionInitFragmentToStartupFragment())
            }
        } else {
            if (!hasDb) {
                // This happens when wiping remotely while the app is in the background and gets
                // restored from the recent app list after wiping and stopping has already
                // completed.
                finish()
                startActivity(Intent(this, WipeCompleteActivity::class.java))
            }
        }
    }

    override fun onActivityResult(result: ActivityResult) {
        // only show next fragment when user went throw onboarding
        // result doesn't matter as we kill the app when user backs out in onboarding
        if (viewModel.needToShowDoNotKillMeFragment) {
            nav.navigate(actionInitFragmentToDoNotKillMeFragment())
        } else {
            nav.navigate(actionInitFragmentToStartupFragment())
        }
    }

    override fun onResume() {
        super.onResume()
        if (needsDozeWhitelisting(this) && viewModel.getAndResetDozeFlag()) {
            showDozeDialog()
        }
    }

    private fun showDozeDialog() = AlertDialog.Builder(this)
        .setMessage(R.string.warning_dozed)
        .setPositiveButton(R.string.fix) { dialog, _ ->
            nav.navigate(actionInitFragmentToDoNotKillMeFragment())
            dialog.dismiss()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()

}
