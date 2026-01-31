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
 *
 * Users can be created via:
 * - Email/password registration (passwordHash is required)
 * - OAuth social login (passwordHash can be null)
 */
data class User(
    val id: UserId,
    val email: String,
    val passwordHash: String?,
    val name: String,
    val avatarUrl: String? = null,
    val createdAt: Instant,
    val lastLoginAt: Instant? = null,
    val isActive: Boolean = true
) {
    init {
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(email.contains("@")) { "Invalid email format" }
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(name.length <= MAX_NAME_LENGTH) { "Name exceeds maximum length of $MAX_NAME_LENGTH" }
    }

    /**
     * Whether this user has a password set (i.e., can login via email/password).
     */
    val hasPassword: Boolean
        get() = !passwordHash.isNullOrBlank()

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

    /**
     * Sets the password for OAuth-only users.
     */
    fun setPassword(passwordHash: String): User {
        require(passwordHash.isNotBlank()) { "Password hash cannot be blank" }
        return copy(passwordHash = passwordHash)
    }

    /**
     * Updates user profile information.
     */
    fun updateProfile(name: String? = null, avatarUrl: String? = null): User {
        return copy(
            name = name?.trim() ?: this.name,
            avatarUrl = avatarUrl ?: this.avatarUrl
        )
    }

    companion object {
        const val MAX_NAME_LENGTH = 100

        /**
         * Creates a new user with email/password.
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

        /**
         * Creates a new user via OAuth (no password).
         */
        fun createFromOAuth(
            email: String,
            name: String,
            avatarUrl: String? = null,
            createdAt: Instant = Instant.now()
        ): User = User(
            id = UserId.generate(),
            email = email.lowercase().trim(),
            passwordHash = null,
            name = name.trim(),
            avatarUrl = avatarUrl,
            createdAt = createdAt
        )
    }
}
