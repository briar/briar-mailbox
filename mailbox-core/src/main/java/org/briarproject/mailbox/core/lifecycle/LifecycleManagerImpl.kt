package org.briarproject.mailbox.core.lifecycle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.briarproject.mailbox.core.db.Database
import org.briarproject.mailbox.core.db.MigrationListener
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.COMPACTING_DATABASE
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.MIGRATING_DATABASE
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.NOT_STARTED
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.RUNNING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STARTING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STARTING_SERVICES
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STOPPED
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STOPPING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.WIPING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.OpenDatabaseHook
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.ALREADY_RUNNING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.SERVICE_ERROR
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.SUCCESS
import org.briarproject.mailbox.core.setup.WipeManager
import org.briarproject.mailbox.core.util.LogUtils.info
import org.briarproject.mailbox.core.util.LogUtils.logDuration
import org.briarproject.mailbox.core.util.LogUtils.logException
import org.briarproject.mailbox.core.util.LogUtils.now
import org.briarproject.mailbox.core.util.LogUtils.trace
import org.slf4j.LoggerFactory.getLogger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Semaphore
import javax.annotation.concurrent.GuardedBy
import javax.annotation.concurrent.ThreadSafe
import javax.inject.Inject
import kotlin.concurrent.thread

@ThreadSafe
internal class LifecycleManagerImpl @Inject constructor(
    private val db: Database,
    private val wipeManager: WipeManager,
) :
    LifecycleManager, MigrationListener {

    companion object {
        private val LOG = getLogger(LifecycleManagerImpl::class.java)
    }

    private val services: MutableList<Service>
    private val openDatabaseHooks: MutableList<OpenDatabaseHook>
    private val executors: MutableList<ExecutorService>

    // This semaphore makes sure that startServices(), stopServices() and wipeMailbox()
    // do not run concurrently. Also all write access to 'state' must happen while this
    // semaphore is being held.
    private val startStopWipeSemaphore = Semaphore(1)
    private val dbLatch = CountDownLatch(1)
    private val startupLatch = CountDownLatch(1)
    private val shutdownLatch = CountDownLatch(1)
    private val state = MutableStateFlow(NOT_STARTED)

    init {
        services = CopyOnWriteArrayList()
        openDatabaseHooks = CopyOnWriteArrayList()
        executors = CopyOnWriteArrayList()
    }

    override fun registerService(s: Service) {
        LOG.info { "Registering service ${s.javaClass.simpleName}" }
        services.add(s)
    }

    override fun registerOpenDatabaseHook(hook: OpenDatabaseHook) {
        LOG.info { "Registering open database hook ${hook.javaClass.simpleName}" }
        openDatabaseHooks.add(hook)
    }

    override fun registerForShutdown(e: ExecutorService) {
        LOG.info { "Registering executor ${e.javaClass.simpleName}" }
        executors.add(e)
    }

    @GuardedBy("startStopWipeSemaphore")
    override fun startServices(): StartResult {
        if (!startStopWipeSemaphore.tryAcquire()) {
            LOG.info("Already starting or stopping")
            return ALREADY_RUNNING
        }
        state.compareAndSet(NOT_STARTED, STARTING)
        return try {
            LOG.info("Opening database")
            var start = now()
            val reopened = db.open(this)
            if (reopened) logDuration(LOG, { "Reopening database" }, start)
            else logDuration(LOG, { "Creating database" }, start)
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
                logDuration(LOG, { "Starting service  ${s.javaClass.simpleName}" }, start)
            }
            state.compareAndSet(STARTING_SERVICES, RUNNING)
            startupLatch.countDown()
            SUCCESS
        } catch (e: ServiceException) {
            logException(LOG, e)
            SERVICE_ERROR
        } finally {
            startStopWipeSemaphore.release()
        }
    }

    // startStopWipeSemaphore is being held during this because it will be called during db.open()
    // in startServices()
    @GuardedBy("startStopWipeSemaphore")
    override fun onDatabaseMigration() {
        state.value = MIGRATING_DATABASE
    }

    // startStopWipeSemaphore is being held during this because it will be called during db.open()
    // in startServices()
    @GuardedBy("startStopWipeSemaphore")
    override fun onDatabaseCompaction() {
        state.value = COMPACTING_DATABASE
    }

    @GuardedBy("startStopWipeSemaphore")
    override fun stopServices() {
        try {
            startStopWipeSemaphore.acquire()
        } catch (e: InterruptedException) {
            LOG.warn("Interrupted while waiting to stop services")
            return
        }
        try {
            if (state.value == STOPPED) {
                return
            }
            val wiped = state.value == WIPING
            LOG.info("Stopping services")
            state.value = STOPPING
            for (s in services) {
                val start = now()
                s.stopService()
                logDuration(LOG, { "Stopping service " + s.javaClass.simpleName }, start)
            }
            for (e in executors) {
                LOG.trace { "Stopping executor ${e.javaClass.simpleName}" }
                e.shutdownNow()
            }
            if (wiped) {
                // If we just wiped, the database has already been closed, so we should not call
                // close(). Since the services are being shut down after wiping (so that the web
                // server can still respond to a wipe request), it is possible that a concurrent
                // API call created some files in the meantime. To make sure we delete those in
                // case of a wipe, repeat deletion of files here after the services have been
                // stopped.
                wipeManager.wipe(wipeDatabase = false)
            } else {
                val start = now()
                db.close()
                logDuration(LOG, { "Closing database" }, start)
            }
            shutdownLatch.countDown()
        } catch (e: ServiceException) {
            logException(LOG, e)
        } finally {
            state.compareAndSet(STOPPING, STOPPED)
            startStopWipeSemaphore.release()
        }
    }

    @GuardedBy("startStopWipeSemaphore")
    override fun wipeMailbox(): Boolean {
        try {
            startStopWipeSemaphore.acquire()
        } catch (e: InterruptedException) {
            LOG.warn("Interrupted while waiting to wipe mailbox")
            return false
        }
        if (!state.compareAndSet(RUNNING, WIPING)) {
            return false
        }
        try {
            wipeManager.wipe(wipeDatabase = true)

            // We need to move this to a thread so that the webserver call can finish when it calls
            // this. Otherwise we'll end up in a deadlock: the same thread trying to stop the
            // webserver from within a call that wants to send a response on the very same webserver.
            // If we were not do this, the webserver would wait for the request to finish and the
            // request would wait for the webserver to finish.
            thread {
                stopServices()
            }

            return true
        } finally {
            startStopWipeSemaphore.release()
        }
    }

    @Throws(InterruptedException::class)
    override fun waitForDatabase() {
        dbLatch.await()
    }

    @Throws(InterruptedException::class)
    override fun waitForStartup() {
        startupLatch.await()
    }

    @Throws(InterruptedException::class)
    override fun waitForShutdown() {
        shutdownLatch.await()
    }

    override fun getLifecycleState(): LifecycleState {
        return state.value
    }

    override fun getLifecycleStateFlow(): StateFlow<LifecycleState> {
        return state
    }
}
