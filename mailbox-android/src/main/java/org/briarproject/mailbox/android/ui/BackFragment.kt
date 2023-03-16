package org.briarproject.mailbox.android.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.CallSuper
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle.State.RESUMED

/**
 * A fragment that allows back navigation via the action bar.
 */
abstract class BackFragment : Fragment(), MenuProvider {
    @CallSuper
    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        // only needed for up/back navigation in action bar
        requireActivity().addMenuProvider(this, viewLifecycleOwner, RESUMED)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    }

    @CallSuper
    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == android.R.id.home) {
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return true
        }
        return false
    }
}
