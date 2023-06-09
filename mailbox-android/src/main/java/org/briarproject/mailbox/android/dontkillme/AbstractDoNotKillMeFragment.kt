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

package org.briarproject.mailbox.android.dontkillme

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import org.briarproject.android.dontkillmelib.DozeUtils.getDozeWhitelistingIntent
import org.briarproject.mailbox.R
import org.briarproject.mailbox.android.dontkillme.DoNotKillMeUtils.showOnboardingDialog
import org.briarproject.mailbox.android.dontkillme.PowerView.OnCheckedChangedListener

abstract class AbstractDoNotKillMeFragment :
    Fragment(), OnCheckedChangedListener, ActivityResultCallback<ActivityResult> {

    private lateinit var dozeView: DozeView
    private lateinit var huaweiProtectedAppsView: HuaweiProtectedAppsView
    private lateinit var huaweiAppLaunchView: HuaweiAppLaunchView
    private lateinit var xiaomiRecentAppsView: XiaomiRecentAppsView
    private lateinit var xiaomiLockAppsView: XiaomiLockAppsView
    private lateinit var next: Button

    private var secondAttempt = false
    private var buttonWasClicked = false
    private val dozeLauncher = registerForActivityResult(StartActivityForResult()) {
        onActivityResult(it)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        requireActivity().title = getString(R.string.dnkm_doze_title)
        setHasOptionsMenu(false)
        val v = inflater.inflate(R.layout.fragment_dont_kill_me, container, false)
        dozeView = v.findViewById(R.id.dozeView)
        dozeView.setOnCheckedChangedListener(this)
        huaweiProtectedAppsView = v.findViewById(R.id.huaweiProtectedAppsView)
        huaweiProtectedAppsView.setOnCheckedChangedListener(this)
        huaweiAppLaunchView = v.findViewById(R.id.huaweiAppLaunchView)
        huaweiAppLaunchView.setOnCheckedChangedListener(this)
        xiaomiRecentAppsView = v.findViewById(R.id.xiaomiRecentAppsView)
        xiaomiRecentAppsView.setOnCheckedChangedListener(this)
        xiaomiLockAppsView = v.findViewById(R.id.xiaomiLockAppsView)
        xiaomiLockAppsView.setOnCheckedChangedListener(this)
        next = v.findViewById(R.id.next)
        val progressBar = v.findViewById<ProgressBar>(R.id.progress)
        dozeView.setOnButtonClickListener { askForDozeWhitelisting() }
        next.setOnClickListener {
            buttonWasClicked = true
            next.visibility = INVISIBLE
            progressBar.visibility = VISIBLE
            onButtonClicked()
        }

        // restore UI state if button was clicked already
        buttonWasClicked = savedInstanceState != null &&
            savedInstanceState.getBoolean("buttonWasClicked", false)
        if (buttonWasClicked) {
            next.visibility = INVISIBLE
            progressBar.visibility = VISIBLE
        }
        return v
    }

    protected abstract fun onButtonClicked()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("buttonWasClicked", buttonWasClicked)
    }

    override fun onActivityResult(result: ActivityResult) {
        // we allow the user to proceed after also denying the second attempt
        if (!dozeView.needsToBeShown() || secondAttempt) {
            dozeView.setChecked(true)
        } else if (context != null) {
            secondAttempt = true
            lifecycleScope.launchWhenResumed {
                // The system state is updated async on Xiaomi devices,
                // so we need to give it a bit of extra time before showing a dialog.
                delay(500)
                showDozeExplanationIfStillNeeded()
            }
        }
    }

    override fun onCheckedChanged() {
        next.isEnabled = dozeView.isChecked() &&
            huaweiProtectedAppsView.isChecked() &&
            huaweiAppLaunchView.isChecked() &&
            xiaomiRecentAppsView.isChecked() &&
            xiaomiLockAppsView.isChecked()
    }

    @SuppressLint("BatteryLife")
    private fun askForDozeWhitelisting() {
        if (context == null) return
        dozeLauncher.launch(getDozeWhitelistingIntent(requireContext()))
    }

    private fun showDozeExplanationIfStillNeeded() {
        if (dozeView.needsToBeShown()) {
            if (context == null) return
            showOnboardingDialog(context, getString(R.string.dnkm_doze_explanation))
        } else {
            dozeView.setChecked(true)
        }
    }
}
