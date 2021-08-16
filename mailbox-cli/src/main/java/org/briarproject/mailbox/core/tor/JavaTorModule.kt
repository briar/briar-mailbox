package org.briarproject.mailbox.core.tor

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.lifecycle.IoExecutor
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.system.Clock
import org.briarproject.mailbox.core.system.LocationUtils
import org.briarproject.mailbox.core.system.ResourceProvider
import org.briarproject.mailbox.core.util.OsUtils.isLinux
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Executor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class JavaTorModule {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(JavaTorModule::class.java)
    }

    @Provides
    @Singleton
    fun provideResourceProvider() = ResourceProvider { name, extension ->
        val cl = javaClass.classLoader
        cl.getResourceAsStream(name + extension)
    }

    @Provides
    @Singleton
    fun provideJavaTorPlugin(
        @IoExecutor ioExecutor: Executor,
        networkManager: NetworkManager,
        locationUtils: LocationUtils,
        clock: Clock,
        resourceProvider: ResourceProvider,
        circumventionProvider: CircumventionProvider,
        backoff: Backoff,
        lifecycleManager: LifecycleManager,
    ): JavaTorPlugin {
        val configDir = File(System.getProperty("user.home") + File.separator + ".config")
        val mailboxDir = File(configDir, ".briar-mailbox")
        val torDir = File(mailboxDir, "tor")
        return JavaTorPlugin(
            ioExecutor,
            networkManager,
            locationUtils,
            clock,
            resourceProvider,
            circumventionProvider,
            backoff,
            architecture,
            torDir,
        ).also { lifecycleManager.registerService(it) }
    }

    private val architecture: String
        get() {
            if (isLinux()) {
                if (LOG.isInfoEnabled) {
                    LOG.info("System's os.arch is ${System.getProperty("os.arch")}")
                }
                when (System.getProperty("os.arch")) {
                    "amd64" -> {
                        return "linux-x86_64"
                    }
                    "aarch64" -> {
                        return "linux-aarch64"
                    }
                    "arm" -> {
                        return "linux-armhf"
                    }
                }
            }
            LOG.info("Tor is not supported on this architecture")
            return "" // TODO
        }

}
