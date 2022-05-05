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
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import org.briarproject.android.dontkillmelib.PowerUtils.needsDozeWhitelisting
import org.briarproject.mailbox.NavOnboardingDirections.actionGlobalStoppingFragment
import org.briarproject.mailbox.NavOnboardingDirections.actionGlobalWipingFragment
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.MailboxService.Companion.EXTRA_STARTUP_FAILED
import org.briarproject.mailbox.android.MailboxService.Companion.EXTRA_START_RESULT
import org.briarproject.mailbox.android.dontkillme.DoNotKillMeFragmentDirections.actionDoNotKillMeFragmentToStartupFragment
import org.briarproject.mailbox.android.ui.InitFragmentDirections.actionInitFragmentToDoNotKillMeFragment
import org.briarproject.mailbox.android.ui.InitFragmentDirections.actionInitFragmentToStartupFragment
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STOPPING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.WIPING
import org.briarproject.mailbox.core.util.LogUtils.info
import org.slf4j.LoggerFactory.getLogger
import kotlin.system.exitProcess

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
            if (complete) nav.navigate(actionDoNotKillMeFragmentToStartupFragment())
        }

        viewModel.wipeComplete.observe(this) { complete ->
            if (complete) {
                startActivity(Intent(this, WipeCompleteActivity::class.java))
            }
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

        if (savedInstanceState == null) {
            viewModel.hasDb.observe(this) { hasDb ->
                if (!hasDb) {
                    startForResult.launch(Intent(this, OnboardingActivity::class.java))
                } else {
                    nav.navigate(actionInitFragmentToStartupFragment())
                }
            }
        }
    }

    override fun onActivityResult(result: ActivityResult?) {
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // will call exitProcess()
        exitIfStartupFailed(intent)
    }

    private fun exitIfStartupFailed(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_STARTUP_FAILED, false)) {
            // Launch StartupFailureActivity in its own process, then exit
            val i = Intent(this, StartupFailureActivity::class.java)
            i.putExtra(EXTRA_START_RESULT, intent.getSerializableExtra(EXTRA_START_RESULT))
            startActivity(i)
            finish()
            LOG.info("Exiting")
            exitProcess(0)
        }
    }

}
