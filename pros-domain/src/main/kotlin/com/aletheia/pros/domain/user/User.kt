package com.aletheia.pros.domain.user

import com.aletheia.pros.domain.common.UserId
import java.time.Instant

/**
 * User domain entity representing a registered user.
 *
 * Users are the primary actors in PROS. Each user has their own:
 * - Thought fragments
 * - Value graph
 * - Decision history
 */
data class User(
    val id: UserId,
    val email: String,
    val passwordHash: String,
    val name: String,
    val createdAt: Instant,
    val lastLoginAt: Instant? = null,
    val isActive: Boolean = true
) {
    init {
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(email.contains("@")) { "Invalid email format" }
        require(passwordHash.isNotBlank()) { "Password hash cannot be blank" }
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(name.length <= MAX_NAME_LENGTH) { "Name exceeds maximum length of $MAX_NAME_LENGTH" }
    }

    /**
     * Updates the last login timestamp.
     */
    fun recordLogin(loginAt: Instant = Instant.now()): User {
        return copy(lastLoginAt = loginAt)
    }

    /**
     * Deactivates the user account.
     */
    fun deactivate(): User {
        return copy(isActive = false)
    }

    companion object {
        const val MAX_NAME_LENGTH = 100

        /**
         * Creates a new user.
         */
        fun create(
            email: String,
            passwordHash: String,
            name: String,
            createdAt: Instant = Instant.now()
        ): User = User(
            id = UserId.generate(),
            email = email.lowercase().trim(),
            passwordHash = passwordHash,
            name = name.trim(),
            createdAt = createdAt
        )
    }
}
