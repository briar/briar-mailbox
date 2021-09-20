package org.briarproject.mailbox.core.lifecycle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.briarproject.mailbox.core.db.DatabaseComponent
import org.briarproject.mailbox.core.db.MigrationListener
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.COMPACTING_DATABASE
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.MIGRATING_DATABASE
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.RUNNING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STARTING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STARTING_SERVICES
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STOPPED
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STOPPING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.OpenDatabaseHook
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.ALREADY_RUNNING
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.SERVICE_ERROR
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.SUCCESS
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
import javax.annotation.concurrent.ThreadSafe
import javax.inject.Inject

@ThreadSafe
internal class LifecycleManagerImpl @Inject constructor(private val db: DatabaseComponent) :
    LifecycleManager, MigrationListener {

    companion object {
        private val LOG = getLogger(LifecycleManagerImpl::class.java)
    }

    private val services: MutableList<Service>
    private val openDatabaseHooks: MutableList<OpenDatabaseHook>
    private val executors: MutableList<ExecutorService>
    private val startStopSemaphore = Semaphore(1)
    private val dbLatch = CountDownLatch(1)
    private val startupLatch = CountDownLatch(1)
    private val shutdownLatch = CountDownLatch(1)
    private val state = MutableStateFlow(STOPPED)

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

    override fun startServices(): StartResult {
        if (!startStopSemaphore.tryAcquire()) {
            LOG.info("Already starting or stopping")
            return ALREADY_RUNNING
        }
        state.compareAndSet(STOPPED, STARTING)
        return try {
            LOG.info("Opening database")
            var start = now()
            val reopened = db.open(this)
            if (reopened) logDuration(LOG, { "Reopening database" }, start)
            else logDuration(LOG, { "Creating database" }, start)
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
            startStopSemaphore.release()
        }
    }

    override fun onDatabaseMigration() {
        state.value = MIGRATING_DATABASE
    }

    override fun onDatabaseCompaction() {
        state.value = COMPACTING_DATABASE
    }

    override fun stopServices() {
        try {
            startStopSemaphore.acquire()
        } catch (e: InterruptedException) {
            LOG.warn("Interrupted while waiting to stop services")
            return
        }
        try {
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
            val start = now()
            db.close()
            logDuration(LOG, { "Closing database" }, start)
            shutdownLatch.countDown()
        } catch (e: ServiceException) {
            logException(LOG, e)
        } finally {
            startStopSemaphore.release()
            state.compareAndSet(STOPPING, STOPPED)
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
