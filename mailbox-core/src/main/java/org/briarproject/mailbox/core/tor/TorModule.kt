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
import org.briarproject.mailbox.core.lifecycle.IoExecutor
import java.util.concurrent.Executor
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class TorModule {

    companion object {
        private const val MIN_POLLING_INTERVAL = 60 * 1000 // 1 minute
        private const val MAX_POLLING_INTERVAL = 10 * 60 * 1000 // 10 mins
        private const val BACKOFF_BASE = 1.2
    }

    @Provides
    @Singleton
    fun provideCircumventionProvider(): CircumventionProvider = object : CircumventionProvider {
        override fun isTorProbablyBlocked(countryCode: String?) = false
        override fun doBridgesWork(countryCode: String?) = true
        override fun needsMeek(countryCode: String?) = false
        override fun getBridges(meek: Boolean): List<String> = emptyList()
    }

    @Provides
    @Singleton
    fun provideBackoff(): Backoff {
        return BackoffImpl(MIN_POLLING_INTERVAL, MAX_POLLING_INTERVAL, BACKOFF_BASE)
    }

    @Provides
    @Singleton
    @IoExecutor
    fun provideIoExecutor(): Executor = ThreadPoolExecutor(
        0,
        Int.MAX_VALUE,
        60,
        TimeUnit.SECONDS,
        SynchronousQueue(),
        DiscardPolicy()
    )

}
