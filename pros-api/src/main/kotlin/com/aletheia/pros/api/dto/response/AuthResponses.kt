package com.aletheia.pros.api.dto.response

import com.aletheia.pros.domain.user.User
import java.time.Instant

/**
 * Response DTO for authentication (login/register).
 */
data class AuthResponse(
    val token: String,
    val user: UserResponse
)

/**
 * Response DTO for user information.
 */
data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    val createdAt: Instant
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id.value.toString(),
                email = user.email,
                name = user.name,
                createdAt = user.createdAt
            )
        }
    }
}
