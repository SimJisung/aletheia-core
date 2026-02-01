package com.aletheia.pros.api.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT Authentication Filter that extracts and validates JWT tokens from requests.
 *
 * Uses request attributes to propagate authentication across async dispatches,
 * which is essential for Kotlin coroutines in Spring MVC.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private const val AUTH_ATTRIBUTE = "com.aletheia.pros.authentication"
    }

    /**
     * Allow filter to run on async dispatches to restore authentication context.
     */
    override fun shouldNotFilterAsyncDispatch(): Boolean = false

    /**
     * Allow filter to run on error dispatches.
     */
    override fun shouldNotFilterErrorDispatch(): Boolean = false

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestUri = request.requestURI
        val method = request.method
        val dispatcherType = request.dispatcherType
        logger.debug("Processing request: $method $requestUri (dispatch: $dispatcherType)")

        // For ASYNC dispatch, restore authentication from request attribute
        if (dispatcherType == jakarta.servlet.DispatcherType.ASYNC) {
            val savedAuth = request.getAttribute(AUTH_ATTRIBUTE) as? UsernamePasswordAuthenticationToken
            if (savedAuth != null) {
                SecurityContextHolder.getContext().authentication = savedAuth
                logger.debug("Restored authentication from request attribute for async dispatch")
                filterChain.doFilter(request, response)
                return
            }
        }

        // Skip if already authenticated
        val existingAuth = SecurityContextHolder.getContext().authentication
        if (existingAuth != null && existingAuth.isAuthenticated && existingAuth.principal is AuthenticatedUser) {
            logger.debug("Already authenticated, skipping JWT validation")
            filterChain.doFilter(request, response)
            return
        }

        try {
            val token = extractToken(request)
            logger.debug("Token extracted: ${if (token != null) "${token.take(20)}..." else "null"}")

            if (token != null && jwtTokenProvider.validateToken(token)) {
                val userId = jwtTokenProvider.getUserIdFromToken(token)
                val email = jwtTokenProvider.getEmailFromToken(token)
                logger.debug("Authentication successful for user: $userId (email: $email)")

                val authentication = UsernamePasswordAuthenticationToken(
                    AuthenticatedUser(userId, email),
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_USER"))
                )

                SecurityContextHolder.getContext().authentication = authentication

                // Save to request attribute for async dispatch restoration
                request.setAttribute(AUTH_ATTRIBUTE, authentication)
            } else {
                logger.debug("No valid token found, continuing without authentication")
            }
        } catch (e: Exception) {
            logger.warn("Authentication failed: ${e.message}", e)
            SecurityContextHolder.clearContext()
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader(AUTHORIZATION_HEADER)
        return if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            bearerToken.substring(BEARER_PREFIX.length)
        } else {
            null
        }
    }
}
