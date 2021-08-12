package org.briarproject.mailbox.android.system

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.android.api.system.AndroidExecutor
import org.briarproject.mailbox.android.api.system.AndroidWakeLockManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class AndroidSystemModule {

    private var scheduledExecutorService: ScheduledExecutorService

    init {
        // Discard tasks that are submitted during shutdown
        val policy: RejectedExecutionHandler = ThreadPoolExecutor.DiscardPolicy()
        scheduledExecutorService = ScheduledThreadPoolExecutor(1, policy)
    }

    @Provides
    @Singleton
    fun provideScheduledExecutorService(
        lifecycleManager: LifecycleManager,
    ): ScheduledExecutorService {
        lifecycleManager.registerForShutdown(scheduledExecutorService)
        return scheduledExecutorService
    }

    @Provides
    @Singleton
    fun provideWakeLockManager(
        wakeLockManager: AndroidWakeLockManagerImpl,
    ): AndroidWakeLockManager {
        return wakeLockManager
    }

    @Provides
    @Singleton
    fun provideAndroidExecutor(androidExecutor: AndroidExecutorImpl): AndroidExecutor {
        return androidExecutor
    }

    @Provides
    @Singleton
    fun provideEventExecutor(androidExecutor: AndroidExecutor): Executor {
        return Executor {
            androidExecutor.runOnUiThread(it)
        }
    }

}
