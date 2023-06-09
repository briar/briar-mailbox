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

import kotlinx.coroutines.flow.MutableStateFlow
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.db.MigrationListener
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.COMPACTING_DATABASE
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.MIGRATING_DATABASE
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.NOT_STARTED
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.RUNNING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STARTING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STARTING_SERVICES
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STOPPING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.WIPING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.OpenDatabaseHook
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.CLOCK_ERROR
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.LIFECYCLE_REUSE
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.SERVICE_ERROR
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.SUCCESS
import org.briarproject.mailbox.core.setup.WipeManager
import org.briarproject.mailbox.core.system.Clock
import org.briarproject.mailbox.core.system.Clock.MAX_REASONABLE_TIME_MS
import org.briarproject.mailbox.core.system.Clock.MIN_REASONABLE_TIME_MS
import org.briarproject.mailbox.core.system.System
import org.briarproject.mailbox.core.util.LogUtils.info
import org.briarproject.mailbox.core.util.LogUtils.logDuration
import org.briarproject.mailbox.core.util.LogUtils.logException
import org.briarproject.mailbox.core.util.LogUtils.now
import org.briarproject.mailbox.core.util.LogUtils.trace
import org.briarproject.mailbox.core.util.LogUtils.warn
import org.slf4j.LoggerFactory.getLogger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import javax.annotation.concurrent.ThreadSafe
import javax.inject.Inject
import kotlin.concurrent.thread

@ThreadSafe
internal class LifecycleManagerImpl @Inject constructor(
    private val db: Database,
    private val wipeManager: WipeManager,
    private val system: System,
    private val clock: Clock,
) :
    LifecycleManager, MigrationListener {

    companion object {
        private val LOG = getLogger(LifecycleManagerImpl::class.java)

        private const val MIN_WIPING_TIME = 1000L
        private const val MIN_STOPPING_TIME = 1000L
    }

    private val services: MutableList<Service>
    private val openDatabaseHooks: MutableList<OpenDatabaseHook>
    private val executors: MutableList<ExecutorService>

    private val dbLatch = CountDownLatch(1)
    private val startupLatch = CountDownLatch(1)
    private val shutdownLatch = CountDownLatch(1)
    private val state = MutableStateFlow(NOT_STARTED)

    private var wipeHook: WipeHook? = null

    init {
        services = CopyOnWriteArrayList()
        openDatabaseHooks = CopyOnWriteArrayList()
        executors = CopyOnWriteArrayList()
    }

    override fun registerService(s: Service) {
        LOG.info { "Registering service ${s.name()}" }
        services.add(s)
    }

    override fun registerOpenDatabaseHook(hook: OpenDatabaseHook) {
        LOG.info { "Registering open database hook ${hook.name()}" }
        openDatabaseHooks.add(hook)
    }

    override fun registerForShutdown(e: ExecutorService) {
        LOG.info { "Registering executor ${e.name()}" }
        executors.add(e)
    }

    override fun startServices(wipeHook: WipeHook): StartResult {
        LOG.info("startServices()")
        LOG.info { "checking state: ${state.value}" }
        if (!state.compareAndSet(NOT_STARTED, STARTING)) {
            LOG.warn { "Invalid state: ${state.value}" }
            return LIFECYCLE_REUSE
        }
        val now = clock.currentTimeMillis()
        if (now < MIN_REASONABLE_TIME_MS || now > MAX_REASONABLE_TIME_MS) {
            LOG.warn { "System clock is unreasonable: $now" }
            return CLOCK_ERROR
        }
        this.wipeHook = wipeHook
        return try {
            LOG.info("Opening database")
            var start = now()
            val reopened = db.open(this)
            if (reopened) logDuration(LOG, start) { "Reopening database" }
            else logDuration(LOG, start) { "Creating database" }
            // Inform hooks that DB was opened
            db.write { txn ->
                for (hook in openDatabaseHooks) {
                    hook.onDatabaseOpened(txn)
                }
            }
            LOG.info("Starting services")
            state.value = STARTING_SERVICES
            dbLatch.countDown()
            for (s in services) {
                start = now()
                s.startService()
                logDuration(LOG, start) { "Starting service  ${s.name()}" }
            }
            state.compareAndSet(STARTING_SERVICES, RUNNING)
            startupLatch.countDown()
            SUCCESS
        } catch (e: ServiceException) {
            logException(LOG, e) { "Error while starting services" }
            SERVICE_ERROR
        }
    }

    // will be called during db.open() in startServices()
    override fun onDatabaseMigration() {
        state.value = MIGRATING_DATABASE
    }

    // will be called during db.open() in startServices()
    override fun onDatabaseCompaction() {
        state.value = COMPACTING_DATABASE
    }

    override fun stopServices(exitAfterStopping: Boolean) {
        LOG.info("stopServices()")
        LOG.info { "checking state: ${state.value}" }
        val wasRunning = state.compareAndSet(RUNNING, STOPPING)
        val wasWiping = state.compareAndSet(WIPING, STOPPING)
        if (!wasRunning && !wasWiping) {
            LOG.warn { "Invalid state: ${state.value}, not stopping" }
            return
        }
        try {
            run("Stopping services and executors", MIN_STOPPING_TIME) {
                LOG.info("Stopping services")
                stopAllServices()
                LOG.info("Stopping executors")
                stopAllExecutors()
            }

            if (wasWiping) {
                // If we just wiped, the database has already been closed, so we should not call
                // close(). Since the services are being shut down after wiping (so that the web
                // server can still respond to a wipe request), it is possible that a concurrent
                // API call created some files in the meantime. To make sure we delete those in
                // case of a wipe, repeat deletion of files here after the services have been
                // stopped.
                run("wiping files again") {
                    wipeManager.wipeFilesOnly()
                    wipeHook?.onWiped()
                }
            } else {
                run("closing database") {
                    db.close()
                }
            }

            shutdownLatch.countDown()
        } finally {
            // Only exit if exitAfterStopping is true because we need to avoid calling exitProcess()
            // on a shutdown hook, which causes a deadlock.
            if (exitAfterStopping) {
                LOG.info("Exiting")
                system.exit(0)
            }
        }
    }

    // will be called during stopServices()
    private fun stopAllServices() {
        for (s in services) {
            run("stopping service ${s.name()}") {
                s.stopService()
            }
        }
    }

    // will be called during stopServices()
    private fun stopAllExecutors() {
        for (e in executors) {
            run("stopping executor ${e.name()}") {
                e.shutdownNow()
            }
        }
    }

    override fun wipeMailbox(): Boolean {
        if (!state.compareAndSet(RUNNING, WIPING)) {
            return false
        }
        run("wiping database and files", MIN_WIPING_TIME) {
            wipeManager.wipeDatabaseAndFiles()
        }
        // We need to move this to a thread so that the webserver call can finish when it calls
        // this. Otherwise we'll end up in a deadlock: the same thread trying to stop the
        // webserver from within a call that wants to send a response on the very same webserver.
        // If we were not do this, the webserver would wait for the request to finish and the
        // request would wait for the webserver to finish.
        thread {
            stopServices(true)
        }
        return true
    }

    @Throws(InterruptedException::class)
    override fun waitForDatabase() = dbLatch.await()

    @Throws(InterruptedException::class)
    override fun waitForStartup() = startupLatch.await()

    @Throws(InterruptedException::class)
    override fun waitForShutdown() = shutdownLatch.await()

    override val lifecycleState = state.value

    override val lifecycleStateFlow = state

    /**
     * Run the task specified, logging the [name] of the task and the time measured it took to
     * execute the task.
     */
    private fun run(name: String, task: () -> Unit) {
        LOG.trace { name }
        val start = now()
        try {
            task()
        } catch (throwable: Throwable) {
            logException(LOG, throwable) { "Error while $name" }
        }
        logDuration(LOG, start) { name }
    }

    /**
     * Same as run above, with the difference that if executing the task took less than [minTime]
     * milliseconds, sleep until at least [minTime] was spent while executing [run].
     * Used to make a task that is expected to finish quickly on fast devices still perceivable
     * by the user.
     */
    private fun run(name: String, minTime: Long, task: () -> Unit) {
        LOG.trace { name }
        val start = now()
        try {
            task()
        } catch (throwable: Throwable) {
            logException(LOG, throwable) { "Error while $name" }
        }
        logDuration(LOG, start) { name }
        val duration = now() - start
        val left = minTime - duration
        if (left > 0) {
            try {
                Thread.sleep(left)
            } catch (e: InterruptedException) {
                // do nothing
            }
        }
    }

    private fun Any.name() = javaClass.simpleName
}
