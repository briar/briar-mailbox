package org.briarproject.mailbox.core.system;

public interface AndroidWakeLock {

    /**
     * Acquires the wake lock. This has no effect if the wake lock has already
     * been acquired.
     */
    void acquire();

    /**
     * Releases the wake lock. This has no effect if the wake lock has already
     * been released.
     */
    void release();
}
