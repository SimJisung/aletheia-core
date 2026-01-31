package com.aletheia.pros.domain.user

import com.aletheia.pros.domain.common.UserId
import java.time.Instant
import java.util.UUID

/**
 * Value object representing an OAuth account ID.
 */
@JvmInline
value class OAuthAccountId(val value: UUID) {
    companion object {
        fun generate(): OAuthAccountId = OAuthAccountId(UUID.randomUUID())
    }
}

/**
 * Domain entity representing an OAuth linked account.
 *
 * Users can have multiple OAuth accounts linked (e.g., both Google and GitHub).
 * This allows users to log in via multiple providers while maintaining a single user identity.
 */
data class OAuthAccount(
    val id: OAuthAccountId,
    val userId: UserId,
    val provider: OAuthProvider,
    val providerUserId: String,
    val email: String?,
    val name: String?,
    val avatarUrl: String?,
    val createdAt: Instant
) {
    init {
        require(providerUserId.isNotBlank()) { "Provider user ID cannot be blank" }
    }

    companion object {
        /**
         * Creates a new OAuth account link.
         */
        fun create(
            userId: UserId,
            provider: OAuthProvider,
            providerUserId: String,
            email: String?,
            name: String?,
            avatarUrl: String?,
            createdAt: Instant = Instant.now()
        ): OAuthAccount = OAuthAccount(
            id = OAuthAccountId.generate(),
            userId = userId,
            provider = provider,
            providerUserId = providerUserId,
            email = email?.lowercase()?.trim(),
            name = name?.trim(),
            avatarUrl = avatarUrl,
            createdAt = createdAt
        )
    }
}
