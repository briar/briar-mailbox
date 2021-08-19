package org.briarproject.mailbox.android.system;

import static org.briarproject.mailbox.core.util.LogUtils.info;
import static org.briarproject.mailbox.core.util.LogUtils.trace;
import static org.briarproject.mailbox.core.util.LogUtils.warn;
import static org.slf4j.LoggerFactory.getLogger;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import org.slf4j.Logger;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
class RenewableWakeLock implements SharedWakeLock {

    private static final Logger LOG = getLogger(RenewableWakeLock.class);

    private final PowerManager powerManager;
    private final ScheduledExecutorService scheduledExecutorService;
    private final int levelAndFlags;
    private final String tag;
    private final long durationMs, safetyMarginMs;

    private final Object lock = new Object();
    @GuardedBy("lock")
    @Nullable
    private WakeLock wakeLock;
    @GuardedBy("lock")
    @Nullable
    private Future<?> future;
    @GuardedBy("lock")
    private int refCount = 0;
    @GuardedBy("lock")
    private long acquired = 0;

    RenewableWakeLock(PowerManager powerManager,
                      ScheduledExecutorService scheduledExecutorService,
                      int levelAndFlags,
                      String tag,
                      long durationMs,
                      long safetyMarginMs) {
        this.powerManager = powerManager;
        this.scheduledExecutorService = scheduledExecutorService;
        this.levelAndFlags = levelAndFlags;
        this.tag = tag;
        this.durationMs = durationMs;
        this.safetyMarginMs = safetyMarginMs;
    }

    @Override
    public void acquire() {
        synchronized (lock) {
            refCount++;
            if (refCount == 1) {
                info(LOG, () -> "Acquiring wake lock " + tag);
                wakeLock = powerManager.newWakeLock(levelAndFlags, tag);
                // We do our own reference counting so we can replace the lock
                // TODO: Check whether using a ref-counted wake lock affects
                //  power management apps
                wakeLock.setReferenceCounted(false);
                wakeLock.acquire(durationMs + safetyMarginMs);
                future = scheduledExecutorService.schedule(this::renew,
                        durationMs, MILLISECONDS);
                acquired = android.os.SystemClock.elapsedRealtime();
            } else {
                trace(LOG, () -> "Wake lock " + tag + " has " + refCount + " holders");
            }
        }
    }

    private void renew() {
        info(LOG, () -> "Renewing wake lock " + tag);
        synchronized (lock) {
            if (wakeLock == null) {
                LOG.info("Already released");
                return;
            }
            trace(LOG, () -> "Wake lock " + tag + " has " + refCount + " holders");
            long now = android.os.SystemClock.elapsedRealtime();
            long expiry = acquired + durationMs + safetyMarginMs;
            if (now > expiry) {
                warn(LOG, () -> "Wake lock expired " + (now - expiry) + " ms ago");
            }
            WakeLock oldWakeLock = wakeLock;
            wakeLock = powerManager.newWakeLock(levelAndFlags, tag);
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire(durationMs + safetyMarginMs);
            oldWakeLock.release();
            future = scheduledExecutorService.schedule(this::renew, durationMs,
                    MILLISECONDS);
            acquired = now;
        }
    }

    @Override
    public void release() {
        synchronized (lock) {
            refCount--;
            if (refCount == 0) {
                info(LOG, () -> "Releasing wake lock " + tag);
                requireNonNull(future).cancel(false);
                future = null;
                requireNonNull(wakeLock).release();
                wakeLock = null;
                acquired = 0;
            } else {
                trace(LOG, () -> "Wake lock " + tag + " has " + refCount + " holders");
            }
        }
    }
}

