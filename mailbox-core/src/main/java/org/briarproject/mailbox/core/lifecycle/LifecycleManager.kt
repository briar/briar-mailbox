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
package org.briarproject.mailbox.core.lifecycle

import kotlinx.coroutines.flow.StateFlow
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.db.Transaction
import org.briarproject.mailbox.core.system.Wakeful
import java.util.concurrent.ExecutorService

/**
 * Manages the lifecycle of the app: opening and closing the
 * [Database] starting and stopping [Services][Service],
 * and shutting down [ExecutorServices][ExecutorService].
 */
interface LifecycleManager {
    /**
     * The result of calling [startServices].
     */
    enum class StartResult {
        SERVICE_ERROR, LIFECYCLE_REUSE, CLOCK_ERROR, SUCCESS
    }

    /**
     * The state the lifecycle can be in.
     * Returned by [lifecycleState]
     */
    enum class LifecycleState {
        NOT_STARTED, STARTING, MIGRATING_DATABASE, COMPACTING_DATABASE, STARTING_SERVICES,
        RUNNING, WIPING, STOPPING, STOPPED;
    }

    /**
     * Registers a hook to be called after the database is opened and before
     * [services][Service] are started. This method should be called
     * before [startServices].
     */
    fun registerOpenDatabaseHook(hook: OpenDatabaseHook)

    /**
     * Registers a [Service] to be started and stopped. This method
     * should be called before [startServices].
     */
    fun registerService(s: Service)

    /**
     * Registers an [ExecutorService] to be shut down. This method
     * should be called before [startServices].
     */
    fun registerForShutdown(e: ExecutorService)

    /**
     * Opens the [Database] using the given key and starts any
     * registered [Services][Service].
     */
    @Wakeful
    fun startServices(wipeHook: WipeHook = WipeHook { }): StartResult

    /**
     * Stops any registered [Services][Service], shuts down any
     * registered [ExecutorServices][ExecutorService], and closes the
     * [Database].
     */
    @Wakeful
    fun stopServices(exitAfterStopping: Boolean)

    /**
     * Wipes entire database as well as stored files. Also stops all services
     * by launching [stopServices] on a new thread after wiping completes.
     *
     * @return true if wiping was successful, false otherwise
     */
    @Wakeful
    fun wipeMailbox(): Boolean

    /**
     * Waits for the [Database] to be opened before returning.
     */
    @Throws(InterruptedException::class)
    fun waitForDatabase()

    /**
     * Waits for the [Database] to be opened and all registered
     * [Services][Service] to start before returning.
     */
    @Throws(InterruptedException::class)
    fun waitForStartup()

    /**
     * Waits for all registered [Services][Service] to stop, all
     * registered [ExecutorServices][ExecutorService] to shut down, and
     * the [Database] to be closed before returning.
     */
    @Throws(InterruptedException::class)
    fun waitForShutdown()

    /**
     * Returns the current state of the lifecycle.
     */
    val lifecycleState: LifecycleState
    val lifecycleStateFlow: StateFlow<LifecycleState>

    interface OpenDatabaseHook {
        /**
         * Called when the database is being opened, before
         * [waitForDatabase] returns.
         *
         *
         * Don't call any methods from the [LifecycleManager] here as
         * this is most likely going to end up in a deadlock.
         */
        @Wakeful
        fun onDatabaseOpened(txn: Transaction)
    }
}
