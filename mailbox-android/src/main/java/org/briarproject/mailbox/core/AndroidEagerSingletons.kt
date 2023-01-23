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

package org.briarproject.mailbox.core

import org.briarproject.mailbox.android.StatusManager
import org.briarproject.mailbox.core.system.AndroidTaskScheduler
import org.briarproject.mailbox.core.system.DozeWatchdog
import org.briarproject.mailbox.core.tor.AndroidNetworkManager
import org.briarproject.mailbox.core.tor.TorPlugin
import javax.inject.Inject

@Suppress("unused")
internal class AndroidEagerSingletons @Inject constructor(
    val androidTaskScheduler: AndroidTaskScheduler,
    val androidNetworkManager: AndroidNetworkManager,
    val torPlugin: TorPlugin,
    val dozeWatchdog: DozeWatchdog,
    val statusManager: StatusManager,
)
