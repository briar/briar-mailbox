package org.briarproject.mailbox.core.tor

import android.content.Context
import android.content.res.Resources
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.event.EventBus
import org.briarproject.mailbox.core.lifecycle.IoExecutor
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.settings.SettingsManager
import org.briarproject.mailbox.core.system.AndroidWakeLockManager
import org.briarproject.mailbox.core.system.Clock
import org.briarproject.mailbox.core.system.LocationUtils
import org.briarproject.mailbox.core.system.ResourceProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.util.concurrent.Executor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class AndroidTorModule {

    companion object {
        private val LOG: Logger = getLogger(AndroidTorModule::class.java)
    }

    @Provides
    @Singleton
    fun provideResourceProvider(@ApplicationContext ctx: Context): ResourceProvider {
        return ResourceProvider { name, _ ->
            val res: Resources = ctx.resources
            // extension is ignored on Android, resources are retrieved without it
            val resId = res.getIdentifier(name, "raw", ctx.packageName)
            res.openRawResource(resId)
        }
    }

    @Provides
    @Singleton
    fun provideAndroidTorPlugin(
        @ApplicationContext app: Context,
        @IoExecutor ioExecutor: Executor,
        settingsManager: SettingsManager,
        networkManager: NetworkManager,
        locationUtils: LocationUtils,
        clock: Clock,
        resourceProvider: ResourceProvider,
        circumventionProvider: CircumventionProvider,
        androidWakeLockManager: AndroidWakeLockManager,
        backoff: Backoff,
        lifecycleManager: LifecycleManager,
        eventBus: EventBus,
    ) = AndroidTorPlugin(
        ioExecutor,
        app,
        settingsManager,
        networkManager,
        locationUtils,
        clock,
        resourceProvider,
        circumventionProvider,
        androidWakeLockManager,
        backoff,
        architecture,
        app.getDir("tor", Context.MODE_PRIVATE),
    ).also {
        lifecycleManager.registerService(it)
        eventBus.addListener(it)
    }

    private val architecture: String?
        get() {
            for (abi in AndroidTorPlugin.getSupportedArchitectures()) {
                return when {
                    abi.startsWith("x86_64") -> "x86_64_pie"
                    abi.startsWith("x86") -> "x86_pie"
                    abi.startsWith("arm64") -> "arm64_pie"
                    abi.startsWith("armeabi") -> "arm_pie"
                    else -> continue
                }
            }
            LOG.info("Tor is not supported on this architecture")
            return null
        }

    @Provides
    fun provideLocationUtils(locationUtils: AndroidLocationUtils): LocationUtils {
        return locationUtils
    }

    @Provides
    @Singleton
    fun provideNetworkManager(
        lifecycleManager: LifecycleManager,
        networkManager: AndroidNetworkManager,
    ): NetworkManager {
        lifecycleManager.registerService(networkManager)
        return networkManager
    }

}
