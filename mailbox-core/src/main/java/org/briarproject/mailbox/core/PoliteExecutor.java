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

package org.briarproject.mailbox.core;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executor;

import javax.annotation.concurrent.GuardedBy;

import static org.briarproject.mailbox.core.util.LogUtils.now;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An {@link Executor} that delegates its tasks to another {@link Executor}
 * while limiting the number of tasks that are delegated concurrently. Tasks
 * are delegated in the order they are submitted to this executor.
 */
public class PoliteExecutor implements Executor {

	private final Object lock = new Object();
	@GuardedBy("lock")
	private final Queue<Runnable> queue = new LinkedList<>();
	private final Executor delegate;
	private final int maxConcurrentTasks;
	private final Logger LOG;

	@GuardedBy("lock")
	private int concurrentTasks = 0;

	/**
	 * @param tag the tag to be used for logging
	 * @param delegate the executor to which tasks will be delegated
	 * @param maxConcurrentTasks the maximum number of tasks that will be
	 * delegated concurrently. If this is set to 1, tasks submitted to this
	 * executor will run in the order they are submitted and will not run
	 * concurrently
	 */
	public PoliteExecutor(String tag, Executor delegate,
			int maxConcurrentTasks) {
		this.delegate = delegate;
		this.maxConcurrentTasks = maxConcurrentTasks;
		LOG = getLogger(tag);
	}

	@Override
	public void execute(@NotNull Runnable r) {
		long submitted = now();
		Runnable wrapped = () -> {
			if (LOG.isDebugEnabled()) {
				long queued = now() - submitted;
				LOG.debug("Queue time " + queued + " ms");
			}
			try {
				r.run();
			} finally {
				scheduleNext();
			}
		};
		synchronized (lock) {
			if (concurrentTasks < maxConcurrentTasks) {
				concurrentTasks++;
				delegate.execute(wrapped);
			} else {
				queue.add(wrapped);
			}
		}
	}

	private void scheduleNext() {
		synchronized (lock) {
			Runnable next = queue.poll();
			if (next == null) concurrentTasks--;
			else delegate.execute(next);
		}
	}
}
