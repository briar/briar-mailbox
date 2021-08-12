package org.briarproject.mailbox.core.lifecycle;

import static org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.COMPACTING_DATABASE;
import static org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.MIGRATING_DATABASE;
import static org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.RUNNING;
import static org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STARTING;
import static org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STARTING_SERVICES;
import static org.briarproject.mailbox.core.lifecycle.LifecycleManager.LifecycleState.STOPPING;
import static org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.ALREADY_RUNNING;
import static org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.SERVICE_ERROR;
import static org.briarproject.mailbox.core.lifecycle.LifecycleManager.StartResult.SUCCESS;
import static org.briarproject.mailbox.core.util.LogUtils.logDuration;
import static org.briarproject.mailbox.core.util.LogUtils.logException;
import static org.briarproject.mailbox.core.util.LogUtils.now;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;

import org.briarproject.mailbox.core.db.DatabaseComponent;
import org.briarproject.mailbox.core.db.MigrationListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

@ThreadSafe
class LifecycleManagerImpl implements LifecycleManager, MigrationListener {

    private static final Logger LOG =
            getLogger(LifecycleManagerImpl.class.getName());

    private final DatabaseComponent db;
    private final List<Service> services;
    private final List<OpenDatabaseHook> openDatabaseHooks;
    private final List<ExecutorService> executors;
    private final Semaphore startStopSemaphore = new Semaphore(1);
    private final CountDownLatch dbLatch = new CountDownLatch(1);
    private final CountDownLatch startupLatch = new CountDownLatch(1);
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    private volatile LifecycleState state = STARTING;

    @Inject
    LifecycleManagerImpl(DatabaseComponent db) {
        this.db = db;
        services = new CopyOnWriteArrayList<>();
        openDatabaseHooks = new CopyOnWriteArrayList<>();
        executors = new CopyOnWriteArrayList<>();
    }

    @Override
    public void registerService(Service s) {
        if (LOG.isLoggable(INFO))
            LOG.info("Registering service " + s.getClass().getSimpleName());
        services.add(s);
    }

    @Override
    public void registerOpenDatabaseHook(OpenDatabaseHook hook) {
        if (LOG.isLoggable(INFO)) {
            LOG.info("Registering open database hook "
                    + hook.getClass().getSimpleName());
        }
        openDatabaseHooks.add(hook);
    }

    @Override
    public void registerForShutdown(ExecutorService e) {
        LOG.info("Registering executor " + e.getClass().getSimpleName());
        executors.add(e);
    }

    @Override
    public StartResult startServices() {
        if (!startStopSemaphore.tryAcquire()) {
            LOG.info("Already starting or stopping");
            return ALREADY_RUNNING;
        }
        try {
            LOG.info("Opening database");
            long start = now();
            boolean reopened = db.open(this);
            if (reopened) logDuration(LOG, "Reopening database", start);
            else logDuration(LOG, "Creating database", start);

            LOG.info("Starting services");
            state = STARTING_SERVICES;
            dbLatch.countDown();

            for (Service s : services) {
                start = now();
                s.startService();
                if (LOG.isLoggable(FINE)) {
                    logDuration(LOG, "Starting service "
                            + s.getClass().getSimpleName(), start);
                }
            }

            state = RUNNING;
            startupLatch.countDown();
            return SUCCESS;
        } catch (ServiceException e) {
            logException(LOG, WARNING, e);
            return SERVICE_ERROR;
        } finally {
            startStopSemaphore.release();
        }
    }

    @Override
    public void onDatabaseMigration() {
        state = MIGRATING_DATABASE;
    }

    @Override
    public void onDatabaseCompaction() {
        state = COMPACTING_DATABASE;
    }

    @Override
    public void stopServices() {
        try {
            startStopSemaphore.acquire();
        } catch (InterruptedException e) {
            LOG.warning("Interrupted while waiting to stop services");
            return;
        }
        try {
            LOG.info("Stopping services");
            state = STOPPING;
            for (Service s : services) {
                long start = now();
                s.stopService();
                if (LOG.isLoggable(FINE)) {
                    logDuration(LOG, "Stopping service "
                            + s.getClass().getSimpleName(), start);
                }
            }
            for (ExecutorService e : executors) {
                if (LOG.isLoggable(FINE)) {
                    LOG.fine("Stopping executor "
                            + e.getClass().getSimpleName());
                }
                e.shutdownNow();
            }
            long start = now();
            db.close();
            logDuration(LOG, "Closing database", start);
            shutdownLatch.countDown();
        } catch (ServiceException e) {
            logException(LOG, WARNING, e);
        } finally {
            startStopSemaphore.release();
        }
    }

    @Override
    public void waitForDatabase() throws InterruptedException {
        dbLatch.await();
    }

    @Override
    public void waitForStartup() throws InterruptedException {
        startupLatch.await();
    }

    @Override
    public void waitForShutdown() throws InterruptedException {
        shutdownLatch.await();
    }

    @Override
    public LifecycleState getLifecycleState() {
        return state;
    }
}
