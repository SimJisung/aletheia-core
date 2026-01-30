package com.aletheia.pros.api.security

import com.aletheia.pros.domain.common.UserId

/**
 * Represents an authenticated user in the security context.
 */
data class AuthenticatedUser(
    val userId: UserId,
    val email: String
)
