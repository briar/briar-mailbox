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

package org.briarproject.mailbox.core.settings

import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.briarproject.mailbox.core.db.DbException
import org.briarproject.mailbox.core.db.Transaction
import org.briarproject.mailbox.core.lifecycle.LifecycleManager
import org.briarproject.mailbox.core.lifecycle.LifecycleManager.OpenDatabaseHook
import org.briarproject.mailbox.core.server.AuthException
import org.briarproject.mailbox.core.server.AuthManager
import org.briarproject.mailbox.core.settings.MetadataManager.Companion.SUPPORTED_VERSIONS
import org.briarproject.mailbox.core.system.Clock
import javax.inject.Inject

interface MetadataManager : OpenDatabaseHook {

    companion object {
        internal val SUPPORTED_VERSIONS = listOf(MailboxVersion(1, 0))
    }

    /**
     * Call this after the owner authenticated.
     * It stores the current timestamp in settings.
     */
    @Throws(DbException::class)
    fun onOwnerConnected()

    /**
     * The epoch timestamp in milliseconds when we saw the last owner connection.
     * Attention: Only access after [LifecycleManager] has finished starting.
     * Will be `0` before or when owner has never connected.
     */
    val ownerConnectionTime: StateFlow<Long>

}

private const val SETTINGS_NAMESPACE_OWNER_METADATA = "ownerMetadata"
private const val SETTINGS_LAST_CONNECTION_TIME = "lastConnectionTime"

class MetadataManagerImpl @Inject constructor(
    private val settingsManager: SettingsManager,
    private val clock: Clock,
) : MetadataManager {

    private val _ownerConnectionTime = MutableStateFlow(0L)
    override val ownerConnectionTime: StateFlow<Long> = _ownerConnectionTime

    override fun onDatabaseOpened(txn: Transaction) {
        val s = settingsManager.getSettings(txn, SETTINGS_NAMESPACE_OWNER_METADATA)
        _ownerConnectionTime.value = s.getLong(SETTINGS_LAST_CONNECTION_TIME, 0L)
    }

    @Throws(DbException::class)
    override fun onOwnerConnected() {
        val timestamp = clock.currentTimeMillis()
        val s = Settings().apply {
            putLong(SETTINGS_LAST_CONNECTION_TIME, timestamp)
        }
        settingsManager.mergeSettings(s, SETTINGS_NAMESPACE_OWNER_METADATA)
        _ownerConnectionTime.value = timestamp
    }

}

class MetadataRouteManager @Inject constructor(
    private val authManager: AuthManager,
) {
    /**
     * GET /status
     */
    @Throws(AuthException::class)
    suspend fun onStatusRequest(call: ApplicationCall) {
        call.respond(HttpStatusCode.OK)
    }

    /**
     * Handler for `GET /versions` API endpoint.
     *
     * Returns supported versions and a 200 status code.
     */
    suspend fun onVersionsRequest(call: ApplicationCall) {
        authManager.assertIsOwner(call.principal())

        val response = VersionsResponse(SUPPORTED_VERSIONS)

        call.respond(HttpStatusCode.OK, response)
    }
}

internal data class MailboxVersion(val major: Int, val minor: Int)

internal data class VersionsResponse(val serverSupports: List<MailboxVersion>)
