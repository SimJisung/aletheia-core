package com.aletheia.pros.api.security

import com.aletheia.pros.domain.common.UserId
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Utility functions for accessing security context.
 */
object SecurityUtils {

    /**
     * Gets the current authenticated user from SecurityContext.
     *
     * @return AuthenticatedUser if authenticated, null otherwise
     */
    fun getCurrentUser(): AuthenticatedUser? {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? AuthenticatedUser
    }

    /**
     * Gets the current user ID from SecurityContext.
     *
     * @return UserId if authenticated
     * @throws IllegalStateException if not authenticated
     */
    fun getCurrentUserId(): UserId {
        return getCurrentUser()?.userId
            ?: throw IllegalStateException("User not authenticated")
    }

    /**
     * Gets the current user email from SecurityContext.
     *
     * @return email if authenticated
     * @throws IllegalStateException if not authenticated
     */
    fun getCurrentUserEmail(): String {
        return getCurrentUser()?.email
            ?: throw IllegalStateException("User not authenticated")
    }
}
