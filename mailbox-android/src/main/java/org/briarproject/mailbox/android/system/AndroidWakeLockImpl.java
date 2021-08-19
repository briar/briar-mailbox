package org.briarproject.mailbox.android.system;

import static org.briarproject.mailbox.core.util.LogUtils.trace;
import static org.slf4j.LoggerFactory.getLogger;

import org.briarproject.mailbox.android.api.system.AndroidWakeLock;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A wrapper around a {@link SharedWakeLock} that provides the more convenient
 * semantics of {@link AndroidWakeLock} (i.e. calls to acquire() and release()
 * don't need to be balanced).
 */
@ThreadSafe
class AndroidWakeLockImpl implements AndroidWakeLock {

    private static final Logger LOG = getLogger(AndroidWakeLockImpl.class);

    private static final AtomicInteger INSTANCE_ID = new AtomicInteger(0);

    private final SharedWakeLock sharedWakeLock;
    private final String tag;

    private final Object lock = new Object();
    @GuardedBy("lock")
    private boolean held = false;

    AndroidWakeLockImpl(SharedWakeLock sharedWakeLock, String tag) {
        this.sharedWakeLock = sharedWakeLock;
        this.tag = tag + "_" + INSTANCE_ID.getAndIncrement();
    }

    @Override
    public void acquire() {
        synchronized (lock) {
            if (held) {
                trace(LOG, () -> tag + " already acquired");
            } else {
                trace(LOG, () -> tag + " acquiring shared wake lock");
                held = true;
                sharedWakeLock.acquire();
            }
        }
    }

    @Override
    public void release() {
        synchronized (lock) {
            if (held) {
                trace(LOG, () -> tag + " releasing shared wake lock");
                held = false;
                sharedWakeLock.release();
            } else {
                trace(LOG, () -> tag + " already released");
            }
        }
    }
}
