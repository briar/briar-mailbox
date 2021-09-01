package org.briarproject.mailbox.core.system;

import android.app.Application;

import org.briarproject.mailbox.core.lifecycle.LifecycleManager;

import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AndroidTaskSchedulerModule {

    @Provides
    @Singleton
    AndroidTaskScheduler provideAndroidTaskScheduler(
            LifecycleManager lifecycleManager, Application app,
            AndroidWakeLockManager wakeLockManager,
            ScheduledExecutorService scheduledExecutorService) {
        AndroidTaskScheduler scheduler = new AndroidTaskScheduler(app,
                wakeLockManager, scheduledExecutorService);
        lifecycleManager.registerService(scheduler);
        return scheduler;
    }

    @Provides
    @Singleton
    TaskScheduler provideTaskScheduler(AndroidTaskScheduler scheduler) {
        return scheduler;
    }
}
