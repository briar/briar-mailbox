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

package org.briarproject.mailbox.core.lifecycle;

import org.briarproject.mailbox.core.db.Database;
import org.briarproject.mailbox.core.db.Transaction;
import org.briarproject.mailbox.core.system.Wakeful;

import java.util.concurrent.ExecutorService;

import kotlinx.coroutines.flow.StateFlow;

/**
 * Manages the lifecycle of the app: opening and closing the
 * {@link Database} starting and stopping {@link Service Services},
 * and shutting down {@link ExecutorService ExecutorServices}.
 */
public interface LifecycleManager {

	/**
	 * The result of calling {@link #startServices()}.
	 */
	enum StartResult {
		ALREADY_RUNNING,
		SERVICE_ERROR,
		SUCCESS
	}

	/**
	 * The state the lifecycle can be in.
	 * Returned by {@link #getLifecycleState()}
	 */
	enum LifecycleState {

		NOT_STARTED,
		STARTING,
		MIGRATING_DATABASE,
		COMPACTING_DATABASE,
		STARTING_SERVICES,
		RUNNING,
		WIPING,
		STOPPING,
		STOPPED;

		public boolean isAfter(LifecycleState state) {
			return ordinal() > state.ordinal();
		}
	}

	/**
	 * Registers a hook to be called after the database is opened and before
	 * {@link Service services} are started. This method should be called
	 * before {@link #startServices()}.
	 */
	void registerOpenDatabaseHook(OpenDatabaseHook hook);

	/**
	 * Registers a {@link Service} to be started and stopped. This method
	 * should be called before {@link #startServices()}.
	 */
	void registerService(Service s);

	/**
	 * Registers an {@link ExecutorService} to be shut down. This method
	 * should be called before {@link #startServices()}.
	 */
	void registerForShutdown(ExecutorService e);

	/**
	 * Opens the {@link Database} using the given key and starts any
	 * registered {@link Service Services}.
	 */
	@Wakeful
	StartResult startServices();

	/**
	 * Stops any registered {@link Service Services}, shuts down any
	 * registered {@link ExecutorService ExecutorServices}, and closes the
	 * {@link Database}.
	 */
	@Wakeful
	void stopServices();

	/**
	 * Wipes entire database as well as stored files. Also stops all services
	 * by launching stopServices() on a new thread after wiping completes.
	 *
	 * @return true if wiping was successful, false otherwise
	 */
	@Wakeful
	boolean wipeMailbox();

	/**
	 * Waits for the {@link Database} to be opened before returning.
	 */
	void waitForDatabase() throws InterruptedException;

	/**
	 * Waits for the {@link Database} to be opened and all registered
	 * {@link Service Services} to start before returning.
	 */
	void waitForStartup() throws InterruptedException;

	/**
	 * Waits for all registered {@link Service Services} to stop, all
	 * registered {@link ExecutorService ExecutorServices} to shut down, and
	 * the {@link Database} to be closed before returning.
	 */
	void waitForShutdown() throws InterruptedException;

	/**
	 * Returns the current state of the lifecycle.
	 */
	LifecycleState getLifecycleState();

	StateFlow<LifecycleState> getLifecycleStateFlow();

	interface OpenDatabaseHook {
		/**
		 * Called when the database is being opened, before
		 * {@link #waitForDatabase()} returns.
		 * <p>
		 * Don't call any methods from the {@link LifecycleManager} here as
		 * this is most likely going to end up in a deadlock.
		 */
		@Wakeful
		void onDatabaseOpened(Transaction txn);
	}
}
