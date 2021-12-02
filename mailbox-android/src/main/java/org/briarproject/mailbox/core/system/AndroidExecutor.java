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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Enables background threads to make Android API calls that must be made from
 * a thread with a message queue.
 */
public interface AndroidExecutor {

	/**
	 * Runs the given task on a background thread with a message queue and
	 * returns a Future for getting the result.
	 */
	<V> Future<V> runOnBackgroundThread(Callable<V> c);

	/**
	 * Runs the given task on a background thread with a message queue.
	 */
	void runOnBackgroundThread(Runnable r);

	/**
	 * Runs the given task on the main UI thread and returns a Future for
	 * getting the result.
	 */
	<V> Future<V> runOnUiThread(Callable<V> c);

	/**
	 * Runs the given task on the main UI thread.
	 */
	void runOnUiThread(Runnable r);
}
