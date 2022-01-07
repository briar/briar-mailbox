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

package org.briarproject.mailbox.core.setup

import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import org.briarproject.mailbox.core.db.DbException
import org.briarproject.mailbox.core.db.Transaction
import org.briarproject.mailbox.core.server.AuthException
import org.briarproject.mailbox.core.server.AuthManager
import org.briarproject.mailbox.core.settings.Settings
import org.briarproject.mailbox.core.settings.SettingsManager
import org.briarproject.mailbox.core.system.RandomIdManager
import javax.inject.Inject

private const val SETTINGS_NAMESPACE_OWNER = "owner"
private const val SETTINGS_SETUP_TOKEN = "setupToken"
private const val SETTINGS_OWNER_TOKEN = "ownerToken"

class SetupManager @Inject constructor(
    private val randomIdManager: RandomIdManager,
    private val settingsManager: SettingsManager,
) {

    /**
     * Stores a new single-use setup token and wipes the owner auth token, if one existed.
     */
    fun restartSetup() {
        val settings = Settings()
        settings[SETTINGS_SETUP_TOKEN] = randomIdManager.getNewRandomId()
        settings[SETTINGS_OWNER_TOKEN] = null
        settingsManager.mergeSettings(settings, SETTINGS_NAMESPACE_OWNER)
    }

    @Throws(DbException::class)
    fun setToken(setupToken: String?, ownerToken: String?) {
        val settings = Settings()
        if (setupToken != null) randomIdManager.assertIsRandomId(setupToken)
        settings[SETTINGS_SETUP_TOKEN] = setupToken
        if (ownerToken != null) randomIdManager.assertIsRandomId(ownerToken)
        settings[SETTINGS_OWNER_TOKEN] = ownerToken
        settingsManager.mergeSettings(settings, SETTINGS_NAMESPACE_OWNER)
    }

    fun getSetupToken(txn: Transaction): String? {
        val settings = settingsManager.getSettings(txn, SETTINGS_NAMESPACE_OWNER)
        return settings[SETTINGS_SETUP_TOKEN]
    }

    @Throws(DbException::class)
    fun getOwnerToken(txn: Transaction): String? {
        val settings = settingsManager.getSettings(txn, SETTINGS_NAMESPACE_OWNER)
        return settings[SETTINGS_OWNER_TOKEN]
    }

}

class SetupRouteManager @Inject constructor(
    private val authManager: AuthManager,
    private val setupManager: SetupManager,
    private val randomIdManager: RandomIdManager,
) {
    /**
     * Handler for `PUT /setup` API endpoint.
     *
     * Wipes setup token and responds with new owner token and 201 status code.
     */
    @Throws(AuthException::class)
    suspend fun onSetupRequest(call: ApplicationCall) {
        authManager.assertIsSetup(call.principal())

        // set new owner token and clear single-use setup token
        val ownerToken = randomIdManager.getNewRandomId()
        setupManager.setToken(null, ownerToken)
        val response = SetupResponse(ownerToken)

        call.respond(HttpStatusCode.Created, response)
    }
}

internal data class SetupResponse(val token: String)
