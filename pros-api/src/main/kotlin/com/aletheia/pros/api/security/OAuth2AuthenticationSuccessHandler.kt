package com.aletheia.pros.api.security

import com.aletheia.pros.application.usecase.auth.OAuthLoginCommand
import com.aletheia.pros.application.usecase.auth.OAuthLoginResult
import com.aletheia.pros.application.usecase.auth.OAuthUseCase
import com.aletheia.pros.domain.user.OAuthProvider
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

private val logger = KotlinLogging.logger {}

/**
 * Handles successful OAuth2 authentication.
 *
 * After successful OAuth login:
 * 1. Extracts user info from OAuth2 provider
 * 2. Creates or links user account via OAuthUseCase
 * 3. Generates JWT token
 * 4. Redirects to frontend with token
 *
 * NOTE: Only active when OAuth2 client is configured
 */
@Component
@ConditionalOnBean(ClientRegistrationRepository::class)
class OAuth2AuthenticationSuccessHandler(
    private val oauthUseCase: OAuthUseCase,
    private val jwtTokenProvider: JwtTokenProvider,
    private val objectMapper: ObjectMapper,
    @Value("\${oauth2.success-redirect-uri:http://localhost:3000/oauth/callback}")
    private val successRedirectUri: String
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauthToken = authentication as OAuth2AuthenticationToken
        val oauth2User = oauthToken.principal
        val registrationId = oauthToken.authorizedClientRegistrationId

        logger.info { "OAuth2 login success for provider: $registrationId" }

        try {
            val provider = OAuthProvider.fromRegistrationId(registrationId)
            val command = extractOAuthCommand(provider, oauth2User, registrationId)

            when (val result = oauthUseCase.loginOrRegister(command)) {
                is OAuthLoginResult.Success -> {
                    val token = jwtTokenProvider.generateToken(result.user.id, result.user.email)

                    val redirectUrl = UriComponentsBuilder.fromUriString(successRedirectUri)
                        .queryParam("token", token)
                        .queryParam("isNewUser", result.isNewUser)
                        .build()
                        .toUriString()

                    logger.info { "OAuth login successful for user: ${result.user.email}, isNewUser: ${result.isNewUser}" }
                    response.sendRedirect(redirectUrl)
                }

                is OAuthLoginResult.EmailRequired -> {
                    logger.warn { "OAuth login failed: email not provided by $registrationId" }
                    redirectWithError(response, "email_required", "Email is required for registration")
                }

                is OAuthLoginResult.AccountDeactivated -> {
                    logger.warn { "OAuth login failed: account deactivated" }
                    redirectWithError(response, "account_deactivated", "Account is deactivated")
                }

                is OAuthLoginResult.UserNotFound -> {
                    logger.error { "OAuth login failed: linked user not found" }
                    redirectWithError(response, "user_not_found", "User account not found")
                }
            }
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Unsupported OAuth provider: $registrationId" }
            redirectWithError(response, "unsupported_provider", "OAuth provider not supported")
        } catch (e: Exception) {
            logger.error(e) { "OAuth login failed with unexpected error" }
            redirectWithError(response, "server_error", "An unexpected error occurred")
        }
    }

    private fun extractOAuthCommand(
        provider: OAuthProvider,
        oauth2User: OAuth2User,
        registrationId: String
    ): OAuthLoginCommand {
        return when (provider) {
            OAuthProvider.GOOGLE -> extractGoogleUser(oauth2User)
            OAuthProvider.GITHUB -> extractGitHubUser(oauth2User)
        }
    }

    private fun extractGoogleUser(oauth2User: OAuth2User): OAuthLoginCommand {
        val attributes = oauth2User.attributes
        return OAuthLoginCommand(
            provider = OAuthProvider.GOOGLE,
            providerUserId = attributes["sub"] as String,
            email = attributes["email"] as? String,
            name = attributes["name"] as? String,
            avatarUrl = attributes["picture"] as? String
        )
    }

    private fun extractGitHubUser(oauth2User: OAuth2User): OAuthLoginCommand {
        val attributes = oauth2User.attributes
        return OAuthLoginCommand(
            provider = OAuthProvider.GITHUB,
            providerUserId = (attributes["id"] as Number).toString(),
            email = attributes["email"] as? String,
            name = attributes["name"] as? String ?: attributes["login"] as? String,
            avatarUrl = attributes["avatar_url"] as? String
        )
    }

    private fun redirectWithError(response: HttpServletResponse, error: String, description: String) {
        val redirectUrl = UriComponentsBuilder.fromUriString(successRedirectUri)
            .queryParam("error", error)
            .queryParam("error_description", description)
            .build()
            .toUriString()

        response.sendRedirect(redirectUrl)
    }
}
