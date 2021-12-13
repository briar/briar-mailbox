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

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.AuthenticationContext
import io.ktor.auth.AuthenticationFailedCause
import io.ktor.auth.AuthenticationFunction
import io.ktor.auth.AuthenticationPipeline
import io.ktor.auth.AuthenticationProvider
import io.ktor.auth.UnauthorizedResponse
import io.ktor.auth.parseAuthorizationHeader
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.request.httpMethod
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineContext
import org.briarproject.mailbox.core.util.LogUtils.debug
import org.slf4j.LoggerFactory.getLogger

private val AUTH_KEY_BEARER: Any = "BearerAuth"
private val LOG = getLogger(BearerAuthenticationProvider::class.java)

internal class BearerAuthenticationProvider constructor(config: Configuration) :
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

    internal class Configuration internal constructor(name: String?) :
        AuthenticationProvider.Configuration(name) {

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
internal fun Authentication.Configuration.bearer(
    name: String? = null,
    configure: BearerAuthenticationProvider.Configuration.() -> Unit,
) {
    val provider = BearerAuthenticationProvider.Configuration(name).apply(configure).build()
    provider.pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        authenticate(context, provider, name)
    }
    register(provider)
}

private suspend fun PipelineContext<AuthenticationContext, ApplicationCall>.authenticate(
    context: AuthenticationContext,
    provider: BearerAuthenticationProvider,
    name: String?,
) {
    val authHeader = provider.authHeader(call)
    if (authHeader == null) {
        context.unauthorizedResponse(AuthenticationFailedCause.NoCredentials, provider)
        return
    }

    try {
        val token = (authHeader as? HttpAuthHeader.Single)?.blob
        if (token == null) {
            context.unauthorizedResponse(AuthenticationFailedCause.InvalidCredentials, provider)
            return
        }

        // TODO remove logging before release
        LOG.debug { "name: $name" }
        LOG.debug { "httpMethod: ${call.request.httpMethod}" }

        val principal = provider.authenticationFunction(call, token)
        if (principal == null) {
            context.unauthorizedResponse(AuthenticationFailedCause.InvalidCredentials, provider)
        } else {
            context.principal(principal)
        }
    } catch (cause: Throwable) {
        val message = cause.message ?: cause.javaClass.simpleName
        LOG.debug { "Bearer verification failed: $message" }
        context.error(AUTH_KEY_BEARER, AuthenticationFailedCause.Error(message))
    }
}

private fun AuthenticationContext.unauthorizedResponse(
    cause: AuthenticationFailedCause,
    provider: BearerAuthenticationProvider,
) {
    challenge(AUTH_KEY_BEARER, cause) {
        call.respond(
            UnauthorizedResponse(
                HttpAuthHeader.Parameterized(
                    authScheme = "Bearer",
                    parameters = mapOf(HttpAuthHeader.Parameters.Realm to provider.realm)
                )
            )
        )
        if (!it.completed && call.response.status() != null) {
            it.complete()
        }
    }
}
