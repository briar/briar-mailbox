package org.briarproject.mailbox.core.system;

import org.briarproject.mailbox.core.lifecycle.LifecycleManager;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class DefaultTaskSchedulerModule {

	private final ScheduledExecutorService scheduledExecutorService;

	public DefaultTaskSchedulerModule() {
		// Discard tasks that are submitted during shutdown
		RejectedExecutionHandler policy =
				new ScheduledThreadPoolExecutor.DiscardPolicy();
		scheduledExecutorService = new ScheduledThreadPoolExecutor(1, policy);
	}

	@Provides
	@Singleton
	TaskScheduler provideTaskScheduler(LifecycleManager lifecycleManager) {
		lifecycleManager.registerForShutdown(scheduledExecutorService);
		return new TaskSchedulerImpl(scheduledExecutorService);
	}
}
