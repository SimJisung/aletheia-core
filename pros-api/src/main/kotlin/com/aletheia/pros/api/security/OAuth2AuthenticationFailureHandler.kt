package com.aletheia.pros.api.security

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

private val logger = KotlinLogging.logger {}

/**
 * Handles OAuth2 authentication failures.
 *
 * Redirects to frontend with error details.
 *
 * NOTE: Only active when OAuth2 client is configured
 */
@Component
@ConditionalOnBean(ClientRegistrationRepository::class)
class OAuth2AuthenticationFailureHandler(
    @Value("\${oauth2.success-redirect-uri:http://localhost:3000/oauth/callback}")
    private val redirectUri: String
) : AuthenticationFailureHandler {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        logger.error(exception) { "OAuth2 authentication failed" }

        val redirectUrl = UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("error", "authentication_failed")
            .queryParam("error_description", exception.localizedMessage ?: "Authentication failed")
            .build()
            .toUriString()

        response.sendRedirect(redirectUrl)
    }
}
