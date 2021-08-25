package org.briarproject.mailbox.core.server

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.Authentication
import io.ktor.auth.AuthenticationContext
import io.ktor.auth.AuthenticationFailedCause
import io.ktor.auth.AuthenticationFunction
import io.ktor.auth.AuthenticationPipeline
import io.ktor.auth.AuthenticationProvider
import io.ktor.auth.Principal
import io.ktor.auth.UnauthorizedResponse
import io.ktor.auth.parseAuthorizationHeader
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.request.ApplicationRequest
import io.ktor.request.httpMethod
import io.ktor.response.respond
import org.briarproject.mailbox.core.util.LogUtils.debug
import org.slf4j.LoggerFactory.getLogger

private val BearerAuthKey: Any = "BearerAuth"
private val LOG = getLogger(BearerAuthenticationProvider::class.java)

internal class BearerAuthenticationProvider constructor(config: Configuration) :
    AuthenticationProvider(config) {

    internal var realm: String = config.realm ?: "Ktor Server"
    internal val authHeader: (ApplicationCall) -> HttpAuthHeader? = config.authHeader
    internal val authenticationFunction = config.authenticationFunction

    internal class Configuration internal constructor(name: String?) :
        AuthenticationProvider.Configuration(name) {

        var realm: String? = null
        val authHeader: (ApplicationCall) -> HttpAuthHeader? = { call ->
            call.request.parseAuthorizationHeaderOrNull()
        }
        var authenticationFunction: AuthenticationFunction<Credentials> = {
            throw NotImplementedError(
                "Bearer auth validate function is not specified." +
                    "Use bearer { validate { ... } } to fix."
            )
        }

        /**
         * Apply [validate] function to every call with [String]
         * @return a principal (usually an instance of [Principal]) or `null`
         */
        fun validate(validate: suspend ApplicationCall.(Credentials) -> Principal?) {
            authenticationFunction = validate
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
        val authHeader = provider.authHeader(call)
        if (authHeader == null) {
            context.unauthorizedResponse(AuthenticationFailedCause.NoCredentials, provider)
            return@intercept
        }

        try {
            // TODO try faking accessType with X-Http-Method-Override header
            val accessType = call.request.httpMethod.toAccessType()
            val token = (authHeader as? HttpAuthHeader.Single)?.blob
            if (accessType == null || token == null) {
                context.unauthorizedResponse(AuthenticationFailedCause.InvalidCredentials, provider)
                return@intercept
            }
            val mailboxId = call.parameters["mailboxId"]

            LOG.debug("name: $name")
            LOG.debug("httpMethod: ${call.request.httpMethod}")

            val credentials = Credentials(accessType, token, mailboxId)
            val principal = provider.authenticationFunction(call, credentials)
            if (principal == null) {
                context.unauthorizedResponse(AuthenticationFailedCause.InvalidCredentials, provider)
            } else {
                context.principal(principal)
            }
        } catch (cause: Throwable) {
            val message = cause.message ?: cause.javaClass.simpleName
            LOG.debug { "Bearer verification failed: $message" }
            context.error(BearerAuthKey, AuthenticationFailedCause.Error(message))
        }
    }
    register(provider)
}

private fun AuthenticationContext.unauthorizedResponse(
    cause: AuthenticationFailedCause,
    provider: BearerAuthenticationProvider,
) {
    challenge(BearerAuthKey, cause) {
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

private fun ApplicationRequest.parseAuthorizationHeaderOrNull() = try {
    parseAuthorizationHeader()
} catch (ex: IllegalArgumentException) {
    LOG.warn("Illegal HTTP auth header", ex)
    null
}
