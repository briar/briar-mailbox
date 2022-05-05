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

import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp
import org.briarproject.mailbox.android.ui.MainActivity
import org.briarproject.mailbox.core.AndroidEagerSingletons
import org.briarproject.mailbox.core.CoreEagerSingletons
import javax.inject.Inject

@HiltAndroidApp
class MailboxApplication : MultiDexApplication() {

    companion object {
        val ENTRY_ACTIVITY = MainActivity::class.java
    }

    @Inject
    internal lateinit var coreEagerSingletons: CoreEagerSingletons

    @Inject
    internal lateinit var androidEagerSingletons: AndroidEagerSingletons

}
