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

package org.briarproject.mailbox.core.system;

import org.briarproject.nullsafety.NotNullByDefault;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A {@link TaskScheduler} that uses a {@link ScheduledExecutorService}.
 */
@ThreadSafe
@NotNullByDefault
class TaskSchedulerImpl implements TaskScheduler {

	private final ScheduledExecutorService scheduledExecutorService;

	TaskSchedulerImpl(ScheduledExecutorService scheduledExecutorService) {
		this.scheduledExecutorService = scheduledExecutorService;
	}

	// TODO: @NonNull should not be needed due to @NotNullByDefault
	@Override
	public Cancellable schedule(Runnable task, Executor executor, long delay,
			TimeUnit unit) {
		Runnable execute = () -> executor.execute(task);
		ScheduledFuture<?> future =
				scheduledExecutorService.schedule(execute, delay, unit);
		return () -> future.cancel(false);
	}

	// TODO: @NonNull should not be needed due to @NotNullByDefault
	@Override
	public Cancellable scheduleWithFixedDelay(Runnable task, Executor executor,
			long delay, long interval, TimeUnit unit) {
		Runnable execute = () -> executor.execute(task);
		ScheduledFuture<?> future = scheduledExecutorService.
				scheduleWithFixedDelay(execute, delay, interval, unit);
		return () -> future.cancel(false);
	}
}
