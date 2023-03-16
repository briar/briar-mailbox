package org.briarproject.mailbox.android.ui.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle.State.RESUMED
import dagger.hilt.android.AndroidEntryPoint
import org.briarproject.mailbox.databinding.ActivitySettingsBinding

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity(), MenuProvider {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // only needed for up/back navigation in action bar
        addMenuProvider(this, this, RESUMED)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    }

    @CallSuper
    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return false
    }
}
