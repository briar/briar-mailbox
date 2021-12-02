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

import java.util.concurrent.Executor;

public interface AndroidWakeLockManager {

	/**
	 * Creates a wake lock with the given tag. The tag is only used for
	 * logging; the underlying OS wake lock will use its own tag.
	 */
	AndroidWakeLock createWakeLock(String tag);

	/**
	 * Runs the given task while holding a wake lock.
	 */
	void runWakefully(Runnable r, String tag);

	/**
	 * Submits the given task to the given executor while holding a wake lock.
	 * The lock is released when the task completes, or if an exception is
	 * thrown while submitting or running the task.
	 */
	void executeWakefully(Runnable r, Executor executor, String tag);

	/**
	 * Starts a dedicated thread to run the given task asynchronously. A wake
	 * lock is acquired before starting the thread and released when the task
	 * completes, or if an exception is thrown while starting the thread or
	 * running the task.
	 * <p>
	 * This method should only be used for lifecycle management tasks that
	 * can't be run on an executor.
	 */
	void executeWakefully(Runnable r, String tag);
}
