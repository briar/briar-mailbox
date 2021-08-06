package org.briarproject.mailbox.core.lifecycle;

import org.briarproject.mailbox.core.db.DatabaseComponent;
import org.briarproject.mailbox.core.system.Wakeful;

import java.util.concurrent.ExecutorService;

/**
 * Manages the lifecycle of the app: opening and closing the
 * {@link DatabaseComponent} starting and stopping {@link Service Services},
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

        STARTING, MIGRATING_DATABASE, COMPACTING_DATABASE, STARTING_SERVICES,
        RUNNING, STOPPING;

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
     * Opens the {@link DatabaseComponent} using the given key and starts any
     * registered {@link Service Services}.
     */
    @Wakeful
    StartResult startServices();

    /**
     * Stops any registered {@link Service Services}, shuts down any
     * registered {@link ExecutorService ExecutorServices}, and closes the
     * {@link DatabaseComponent}.
     */
    @Wakeful
    void stopServices();

    /**
     * Waits for the {@link DatabaseComponent} to be opened before returning.
     */
    void waitForDatabase() throws InterruptedException;

    /**
     * Waits for the {@link DatabaseComponent} to be opened and all registered
     * {@link Service Services} to start before returning.
     */
    void waitForStartup() throws InterruptedException;

    /**
     * Waits for all registered {@link Service Services} to stop, all
     * registered {@link ExecutorService ExecutorServices} to shut down, and
     * the {@link DatabaseComponent} to be closed before returning.
     */
    void waitForShutdown() throws InterruptedException;

    /**
     * Returns the current state of the lifecycle.
     */
    LifecycleState getLifecycleState();

    interface OpenDatabaseHook {
        /**
         * Called when the database is being opened, before
         * {@link #waitForDatabase()} returns.
         */
        @Wakeful
        void onDatabaseOpened();
    }
}