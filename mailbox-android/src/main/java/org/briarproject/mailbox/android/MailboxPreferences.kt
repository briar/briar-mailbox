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

package org.briarproject.mailbox.android

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.briarproject.mailbox.android.ui.WipeCompleteFragment
import javax.inject.Inject

class MailboxPreferences @Inject constructor(@ApplicationContext val context: Context) {

    companion object {
        private const val AUTO_START_ENABLED = "auto-start-enabled"
        private const val WIPED_LOCALLY = "wiped-locally"
    }

    private var preferences: SharedPreferences = getDefaultSharedPreferences(context)

    /**
     * Get if auto-start is currently enabled. It gets enabled when the user manually starts the
     * app and gets disabled when they stop the app. [StartReceiver] checks this flag upon boot and
     * decides based on the flag whether it starts the lifecycle. As a result, a reboot of the
     * device will boot the mailbox if it has been running before reboot and will not boot it if it
     * has not been running before reboot.
     */
    fun isAutoStartEnabled(): Boolean {
        return preferences.getBoolean(AUTO_START_ENABLED, true)
    }

    fun setAutoStartEnabled(enabled: Boolean) {
        preferences.edit {
            putBoolean(AUTO_START_ENABLED, enabled)
        }
    }

    /**
     * Set a flag from the status fragment when wiping is initiated locally, not remotely from
     * Briar. Can be used to find out in [WipeCompleteFragment] whether wiping has been initiated
     * remotely or locally.
     */
    fun setWipedLocally() {
        preferences.edit {
            putBoolean(WIPED_LOCALLY, true)
        }
    }

    /**
     * Find out whether wiping has been initiated locally or remotely. See [setWipedLocally].
     */
    fun hasBeenWipedLocally(): Boolean {
        return preferences.getBoolean(WIPED_LOCALLY, false)
    }

    /**
     * Unset the flag set using [setWipedLocally]. Restores the situation of a fresh install
     * where the flag is neither true or false, just unset.
     */
    fun unsetWipedLocally() {
        preferences.edit {
            remove(WIPED_LOCALLY)
        }
    }
}
