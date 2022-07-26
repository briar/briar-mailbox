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

package org.briarproject.mailbox.core.server

import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.AuthenticationFunction
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.auth.UnauthorizedResponse
import io.ktor.server.auth.parseAuthorizationHeader
import io.ktor.server.response.respond
import org.briarproject.mailbox.core.util.LogUtils.debug
import org.slf4j.LoggerFactory.getLogger

private val AUTH_KEY_BEARER: Any = "BearerAuth"
private val LOG = getLogger(BearerAuthenticationProvider::class.java)

internal class BearerAuthenticationProvider constructor(config: Config) :
    AuthenticationProvider(config) {

    internal val realm: String = "Briar Mailbox"
    internal val authHeader: (ApplicationCall) -> HttpAuthHeader? = { call ->
        try {
            call.request.parseAuthorizationHeader()
        } catch (ex: IllegalArgumentException) {
            LOG.warn("Illegal HTTP auth header", ex)
            null
        }
    }
    internal val authenticationFunction = config.authenticationFunction

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val authHeader = authHeader(call)
        if (authHeader == null) {
            context.unauthorizedResponse(AuthenticationFailedCause.NoCredentials, this)
            return
        }

        try {
            val token = (authHeader as? HttpAuthHeader.Single)?.blob
            if (token == null) {
                context.unauthorizedResponse(AuthenticationFailedCause.InvalidCredentials, this)
                return
            }

            val principal = authenticationFunction(call, token)
            if (principal == null) {
                context.unauthorizedResponse(AuthenticationFailedCause.InvalidCredentials, this)
            } else {
                context.principal(principal)
            }
        } catch (cause: Throwable) {
            val message = cause.message ?: cause.javaClass.simpleName
            LOG.debug { "Bearer verification failed: $message" }
            context.error(AUTH_KEY_BEARER, AuthenticationFailedCause.Error(message))
        }
    }

    internal class Config internal constructor(name: String?) :
        AuthenticationProvider.Config(name) {

        /**
         * This function is applied to every call with a [String] auth token.
         * @return a [MailboxPrincipal] or `null`
         */
        var authenticationFunction: AuthenticationFunction<String> = {
            throw NotImplementedError(
                "Bearer auth authenticationFunction is not specified." +
                    "Use bearer { authenticationFunction = { ... } } to fix."
            )
        }

        internal fun build() = BearerAuthenticationProvider(this)
    }

}

/**
 * Installs Bearer token Authentication mechanism
 */
internal fun AuthenticationConfig.bearer(
    name: String? = null,
    configure: BearerAuthenticationProvider.Config.() -> Unit,
) {
    val provider = BearerAuthenticationProvider.Config(name).apply(configure).build()
    register(provider)
}

private fun AuthenticationContext.unauthorizedResponse(
    cause: AuthenticationFailedCause,
    provider: BearerAuthenticationProvider,
) {
    challenge(AUTH_KEY_BEARER, cause) { challenge, call ->
        call.respond(
            UnauthorizedResponse(
                HttpAuthHeader.Parameterized(
                    authScheme = "Bearer",
                    parameters = mapOf(HttpAuthHeader.Parameters.Realm to provider.realm)
                )
            )
        )
        if (!challenge.completed && call.response.status() != null) {
            challenge.complete()
        }
    }
}
