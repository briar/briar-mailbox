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

package org.briarproject.mailbox.core

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.briarproject.mailbox.core.db.DatabaseModule
import org.briarproject.mailbox.core.event.EventModule
import org.briarproject.mailbox.core.files.FileModule
import org.briarproject.mailbox.core.lifecycle.LifecycleModule
import org.briarproject.mailbox.core.server.WebServerModule
import org.briarproject.mailbox.core.settings.SettingsModule
import org.briarproject.mailbox.core.setup.SetupModule
import org.briarproject.mailbox.core.system.Clock
import org.briarproject.mailbox.core.tor.TorModule
import javax.inject.Singleton

@Module(
    includes = [
        EventModule::class,
        LifecycleModule::class,
        DatabaseModule::class,
        FileModule::class,
        SetupModule::class,
        WebServerModule::class,
        SettingsModule::class,
        TorModule::class,
    ]
)
@InstallIn(SingletonComponent::class)
class CoreModule {
    @Singleton
    @Provides
    fun provideClock() = Clock { System.currentTimeMillis() }
}
