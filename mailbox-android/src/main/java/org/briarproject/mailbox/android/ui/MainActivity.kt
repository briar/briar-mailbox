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
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.android.dontkillmelib.DozeUtils.needsDozeWhitelisting
import org.briarproject.mailbox.NavMainDirections.actionGlobalClockSkewFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalDoNotKillMeFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalNoNetworkFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalOnboardingContainer
import org.briarproject.mailbox.NavMainDirections.actionGlobalQrCodeFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalSetupCompleteFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalStartupFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalStatusFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalStoppingFragment
import org.briarproject.mailbox.NavMainDirections.actionGlobalWipingFragment
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.StatusManager.ErrorClockSkew
import org.briarproject.mailbox.android.StatusManager.ErrorNoNetwork
import org.briarproject.mailbox.android.StatusManager.MailboxAppState
import org.briarproject.mailbox.android.StatusManager.NeedOnboarding
import org.briarproject.mailbox.android.StatusManager.NeedsDozeExemption
import org.briarproject.mailbox.android.StatusManager.NotStarted
import org.briarproject.mailbox.android.StatusManager.StartedSettingUp
import org.briarproject.mailbox.android.StatusManager.StartedSetupComplete
import org.briarproject.mailbox.android.StatusManager.Starting
import org.briarproject.mailbox.android.StatusManager.Stopped
import org.briarproject.mailbox.android.StatusManager.Stopping
import org.briarproject.mailbox.android.StatusManager.Undecided
import org.briarproject.mailbox.android.StatusManager.Wiping
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.NOT_STARTED
import org.briarproject.mailbox.core.util.LogUtils.info
import org.slf4j.LoggerFactory.getLogger

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        private val LOG = getLogger(MainActivity::class.java)

        const val BUNDLE_LIFECYCLE_HAS_STARTED = "LIFECYCLE_HAS_STARTED"
    }

    private val viewModel: MailboxViewModel by viewModels()
    private val nav: NavController by lazy {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
        navHostFragment.navController
    }

    private var hadBeenStartedOnSave = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LOG.info("onCreate()")
        setContentView(R.layout.activity_main)

        LOG.info { "do we have a saved instance state? " + (savedInstanceState != null) }
        hadBeenStartedOnSave =
            savedInstanceState?.getBoolean(BUNDLE_LIFECYCLE_HAS_STARTED) ?: false

        // set action bar titles based on navigation destination
        nav.addOnDestinationChangedListener { _, destination, _ ->
            // never show the up indicator. exceptions below
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            title = when (destination.id) {
                R.id.qrCodeFragment -> getString(R.string.link_title)
                R.id.qrCodeLinkFragment -> {
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    getString(R.string.link_text_title)
                }
                R.id.statusFragment -> getString(R.string.app_name)
                else -> ""
            }
        }

        launchAndRepeatWhileStarted {
            viewModel.appState.collect { onAppStateChanged(it) }
        }
    }

    private fun onAppStateChanged(state: MailboxAppState) {
        // Catch the situation where we come back to the activity after a remote wipe has happened
        // while the app was in the background and gets restored from the recent app list after
        // wiping and stopping has already completed.
        // In this case onSaveInstanceState() has written true to the bundle but there's no db
        // any longer.
        if (hadBeenStartedOnSave && state == NeedOnboarding) {
            finish()
            startActivity(Intent(this, WipeCompleteActivity::class.java))
            return
        }
        when (state) {
            Undecided -> supportActionBar?.hide() // hide action bar until we need it
            NeedOnboarding -> {
                supportActionBar?.hide()
                if (nav.currentDestination?.id == R.id.initFragment)
                    nav.navigate(actionGlobalOnboardingContainer())
            }
            NeedsDozeExemption -> {
                supportActionBar?.hide()
                if (nav.currentDestination?.id != R.id.doNotKillMeFragment)
                    nav.navigate(actionGlobalDoNotKillMeFragment())
            }
            NotStarted -> {
                supportActionBar?.hide()
                nav.navigate(actionGlobalStartupFragment())
            }
            // It is important to navigate here from various fragments. The normal case is
            // that we come from the init fragment, do-not-kill fragment or the onboarding fragment.
            // However, when the service got killed and the app has been restored with a different
            // UI state such as the qr code screen or the status screen, then we also want to
            // navigate to the startup fragment.
            is Starting -> {
                supportActionBar?.hide()
                if (nav.currentDestination?.id != R.id.startupFragment)
                    nav.navigate(actionGlobalStartupFragment())
            }
            is StartedSettingUp -> {
                supportActionBar?.show()
                if (nav.currentDestination?.id != R.id.qrCodeFragment &&
                    nav.currentDestination?.id != R.id.qrCodeLinkFragment
                ) nav.navigate(actionGlobalQrCodeFragment())
            }
            StartedSetupComplete -> {
                if (nav.currentDestination?.id == R.id.qrCodeFragment) {
                    supportActionBar?.hide()
                    nav.navigate(actionGlobalSetupCompleteFragment())
                } else if (nav.currentDestination?.id != R.id.statusFragment &&
                    nav.currentDestination?.id != R.id.setupCompleteFragment
                ) {
                    supportActionBar?.show()
                    nav.navigate(actionGlobalStatusFragment())
                }
            }
            ErrorNoNetwork -> {
                supportActionBar?.hide()
                if (nav.currentDestination?.id != R.id.noNetworkFragment)
                    nav.navigate(actionGlobalNoNetworkFragment())
            }
            ErrorClockSkew -> {
                supportActionBar?.hide()
                if (nav.currentDestination?.id != R.id.clockSkewFragment)
                    nav.navigate(actionGlobalClockSkewFragment())
            }
            Stopping -> {
                supportActionBar?.hide()
                if (nav.currentDestination?.id != R.id.stoppingFragment)
                    nav.navigate(actionGlobalStoppingFragment())
            }
            Wiping -> {
                supportActionBar?.hide()
                if (nav.currentDestination?.id != R.id.wipingFragment)
                    nav.navigate(actionGlobalWipingFragment())
            }
            Stopped -> {} // nothing to do but needs to be exhaustive for Kotlin 1.7
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(
            BUNDLE_LIFECYCLE_HAS_STARTED,
            viewModel.lifecycleState.value != NOT_STARTED
        )
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
            viewModel.onNeedsDozeExemption()
            dialog.dismiss()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()

}
