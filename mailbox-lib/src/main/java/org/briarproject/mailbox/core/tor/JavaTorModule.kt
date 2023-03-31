/*
 *     Briar Mailbox
 *     Copyright (C) 2021-2022  The Briar Project
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.briarproject.mailbox.core.tor

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.event.EventBus
import org.briarproject.mailbox.core.event.EventExecutor
import org.briarproject.mailbox.core.files.FileProvider
import org.briarproject.mailbox.core.lifecycle.IoExecutor
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.server.WebServerManager
import org.briarproject.mailbox.core.settings.SettingsManager
import org.briarproject.mailbox.core.system.LocationUtils
import org.briarproject.mailbox.core.util.OsUtils.isLinux
import org.briarproject.onionwrapper.CircumventionProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import java.io.File
import java.util.Locale
import java.util.concurrent.Executor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class JavaTorModule {

    companion object {
        private val LOG: Logger = getLogger(JavaTorModule::class.java)
    }

    @Provides
    @Singleton
    fun provideJavaTorPlugin(
        @IoExecutor ioExecutor: Executor,
        @EventExecutor eventExecutor: Executor,
        settingsManager: SettingsManager,
        networkManager: NetworkManager,
        locationUtils: LocationUtils,
        circumventionProvider: CircumventionProvider,
        lifecycleManager: LifecycleManager,
        eventBus: EventBus,
        fileProvider: FileProvider,
        webServerManager: WebServerManager,
    ): TorPlugin {
        val torDir = File(fileProvider.root, "tor")
        return JavaTorPlugin(
            ioExecutor,
            eventExecutor,
            settingsManager,
            networkManager,
            locationUtils,
            circumventionProvider,
            architecture,
            torDir
        ) { webServerManager.port }.also {
            lifecycleManager.registerService(it)
            eventBus.addListener(it)
        }
    }

    private val architecture: String?
        get() {
            if (isLinux()) {
                if (LOG.isInfoEnabled) {
                    LOG.info("System's os.arch is ${System.getProperty("os.arch")}")
                }
                when (System.getProperty("os.arch")) {
                    "amd64" -> {
                        return "x86_64"
                    }
                    "aarch64" -> {
                        return "aarch64"
                    }
                    "arm" -> {
                        return "armhf"
                    }
                }
            }
            LOG.info("Tor is not supported on this architecture")
            return null
        }

    @Provides
    @Singleton
    fun provideNetworkManager(networkManager: MailboxLibNetworkManager): NetworkManager {
        return networkManager
    }

    @Provides
    @Singleton
    fun provideLocationUtils() = LocationUtils { Locale.getDefault().country }

}
