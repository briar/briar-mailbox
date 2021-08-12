package org.briarproject.mailbox.android.api.system;

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
