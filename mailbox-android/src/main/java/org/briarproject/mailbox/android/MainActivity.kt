package org.briarproject.mailbox.android

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.android.dontkillmelib.PowerUtils.needsDozeWhitelisting
import org.briarproject.mailbox.R

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MailboxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.doNotKillComplete.observe(this) { complete ->
            if (complete) showFragment(MainFragment())
        }

        if (savedInstanceState == null) {
            val f = if (viewModel.needToShowDoNotKillMeFragment) {
                DoNotKillMeFragment()
            } else {
                MainFragment()
            }
            showFragment(f)
        }
    }

    override fun onResume() {
        super.onResume()
        if (needsDozeWhitelisting(this) && viewModel.getAndResetDozeFlag()) {
            showDozeDialog()
        }
    }

    private fun showFragment(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, f)
            .commitNow()
    }

    private fun showDozeDialog() = AlertDialog.Builder(this)
        .setMessage(R.string.warning_dozed)
        .setPositiveButton(R.string.fix) { dialog, _ ->
            showFragment(DoNotKillMeFragment())
            dialog.dismiss()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()

}
