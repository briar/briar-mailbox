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

import dagger.Module
import dagger.Provides
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
class TestTaskSchedulerModule {

    /**
     * @return a [TaskScheduler] that runs everything immediately
     * without actually scheduling anything.
     */
    @Provides
    @Singleton
    fun provideTaskScheduler(): TaskScheduler = object : TaskScheduler {
        private val cancellable = object : TaskScheduler.Cancellable {
            override fun cancel() {}
        }

        override fun schedule(
            task: Runnable,
            executor: Executor,
            delay: Long,
            unit: TimeUnit,
        ): TaskScheduler.Cancellable {
            executor.execute(task)
            return cancellable
        }

        override fun scheduleWithFixedDelay(
            task: Runnable,
            executor: Executor,
            delay: Long,
            interval: Long,
            unit: TimeUnit,
        ): TaskScheduler.Cancellable {
            executor.execute(task)
            return cancellable
        }
    }
}
