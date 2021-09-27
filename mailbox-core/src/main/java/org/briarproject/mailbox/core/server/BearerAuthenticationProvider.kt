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
import io.ktor.request.httpMethod
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineContext
import org.briarproject.mailbox.core.util.LogUtils.debug
import org.slf4j.LoggerFactory.getLogger

private val AUTH_KEY_BEARER: Any = "BearerAuth"
private val LOG = getLogger(BearerAuthenticationProvider::class.java)

internal class BearerAuthenticationProvider constructor(config: Configuration) :
    AuthenticationProvider(config) {

    internal var realm: String = config.realm ?: "Ktor Server"
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

        var realm: String? = null

        /**
         * This function is applied to every call with [Credentials].
         * @return a principal (usually an instance of [Principal]) or `null`
         */
        var authenticationFunction: AuthenticationFunction<Credentials> = {
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
        // TODO try faking accessType with X-Http-Method-Override header
        val accessType = call.request.httpMethod.toAccessType()
        val token = (authHeader as? HttpAuthHeader.Single)?.blob
        if (accessType == null || token == null) {
            context.unauthorizedResponse(AuthenticationFailedCause.InvalidCredentials, provider)
            return
        }
        val folderId = call.parameters["folderId"]

        // TODO remove logging before release
        LOG.debug { "name: $name" }
        LOG.debug { "httpMethod: ${call.request.httpMethod}" }

        val credentials = Credentials(accessType, token, folderId)
        val principal = provider.authenticationFunction(call, credentials)
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
