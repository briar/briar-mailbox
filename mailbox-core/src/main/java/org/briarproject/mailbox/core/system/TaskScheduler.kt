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
package org.briarproject.mailbox.core.system

import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * A service that can be used to schedule the execution of tasks.
 */
interface TaskScheduler {
    /**
     * Submits the given task to the given executor after the given delay.
     *
     * If the platform supports wake locks, a wake lock will be held while
     * submitting and running the task.
     */
    fun schedule(task: Runnable, executor: Executor, delay: Long, unit: TimeUnit): Cancellable

    /**
     * Submits the given task to the given executor after the given delay,
     * and then repeatedly with the given interval between executions
     * (measured from the end of one execution to the beginning of the next).
     *
     * If the platform supports wake locks, a wake lock will be held while
     * submitting and running the task.
     */
    fun scheduleWithFixedDelay(
        task: Runnable,
        executor: Executor,
        delay: Long,
        interval: Long,
        unit: TimeUnit,
    ): Cancellable

    interface Cancellable {
        /**
         * Cancels the task if it has not already started running. If the task
         * is periodic ([scheduleWithFixedDelay]),
         * all future executions of the task are cancelled.
         */
        fun cancel()
    }
}
