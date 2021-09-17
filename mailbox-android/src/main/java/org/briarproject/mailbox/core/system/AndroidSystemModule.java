package org.briarproject.mailbox.core.system;

import org.briarproject.mailbox.core.event.EventExecutor;
import org.briarproject.mailbox.core.lifecycle.LifecycleManager;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AndroidSystemModule {

	private final ScheduledExecutorService scheduledExecutorService;

	public AndroidSystemModule() {
		// Discard tasks that are submitted during shutdown
		RejectedExecutionHandler policy =
				new ScheduledThreadPoolExecutor.DiscardPolicy();
		scheduledExecutorService = new ScheduledThreadPoolExecutor(1, policy);
	}

	@Provides
	@Singleton
	ScheduledExecutorService provideScheduledExecutorService(
			LifecycleManager lifecycleManager) {
		lifecycleManager.registerForShutdown(scheduledExecutorService);
		return scheduledExecutorService;
	}

	@Provides
	@Singleton
	AndroidExecutor provideAndroidExecutor(
			AndroidExecutorImpl androidExecutor) {
		return androidExecutor;
	}

	@Provides
	@Singleton
	@EventExecutor
	Executor provideEventExecutor(AndroidExecutor androidExecutor) {
		return androidExecutor::runOnUiThread;
	}

	@Provides
	@Singleton
	AndroidWakeLockManager provideWakeLockManager(
			AndroidWakeLockManagerImpl wakeLockManager) {
		return wakeLockManager;
	}
}
