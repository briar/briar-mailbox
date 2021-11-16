package org.briarproject.mailbox.core.setup

import io.ktor.application.ApplicationCall
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import org.briarproject.mailbox.core.db.DbException
import org.briarproject.mailbox.core.db.Transaction
import org.briarproject.mailbox.core.server.AuthException
import org.briarproject.mailbox.core.server.MailboxPrincipal
import org.briarproject.mailbox.core.server.MailboxPrincipal.SetupPrincipal
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

    /**
     * Handler for `PUT /setup` API endpoint.
     *
     * Wipes setup token and responds with new owner token and 201 status code.
     */
    @Throws(AuthException::class)
    suspend fun onSetupRequest(call: ApplicationCall) {
        val principal: MailboxPrincipal? = call.principal()
        if (principal !is SetupPrincipal) throw AuthException()

        // set new owner token and clear single-use setup token
        val ownerToken = randomIdManager.getNewRandomId()
        setToken(null, ownerToken)
        val response = SetupResponse(ownerToken)

        call.respond(HttpStatusCode.Created, response)
    }

    /**
     * Visible for testing, consider private.
     */
    @Throws(DbException::class)
    internal fun setToken(setupToken: String?, ownerToken: String?) {
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

internal data class SetupResponse(val token: String)
