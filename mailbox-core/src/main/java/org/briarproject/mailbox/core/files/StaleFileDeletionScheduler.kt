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

package org.briarproject.mailbox.core.files

import org.briarproject.mailbox.core.lifecycle.IoExecutor
import org.briarproject.mailbox.core.lifecycle.Service
import org.briarproject.mailbox.core.system.TaskScheduler
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Inject

/**
 * Attempt to delete stale files after this initial delay has passed.
 */
private val STALE_FILE_DELETION_DELAY = MINUTES.toMillis(3)

/**
 * Schedule a new deletion after this many milliseconds have passed.
 */
private val STALE_FILE_DELETION_INTERVAL = DAYS.toMillis(1)

interface StaleFileDeletionScheduler : Service

/**
 * Schedules the deletion of stale files via the [FileManager] after the lifecycle has started.
 */
class StaleFileDeletionSchedulerImpl @Inject constructor(
    private val fileManager: FileManager,
    private val taskScheduler: TaskScheduler,
    @IoExecutor private val ioExecutor: Executor,
) : StaleFileDeletionScheduler {

    private var task: TaskScheduler.Cancellable? = null

    override fun startService() {
        // schedule the deletion of stale files
        task = taskScheduler.scheduleWithFixedDelay(
            task = fileManager::deleteStaleFiles,
            executor = ioExecutor,
            delay = STALE_FILE_DELETION_DELAY,
            interval = STALE_FILE_DELETION_INTERVAL,
            unit = MILLISECONDS,
        )
    }

    override fun stopService() {
        task?.cancel()
    }
}
