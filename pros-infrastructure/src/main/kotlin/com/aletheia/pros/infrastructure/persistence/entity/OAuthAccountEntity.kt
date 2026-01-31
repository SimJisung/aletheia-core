package com.aletheia.pros.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * JPA Entity for OAuth linked accounts.
 */
@Entity
@Table(
    name = "oauth_accounts",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_oauth_provider_user",
            columnNames = ["provider", "provider_user_id"]
        )
    ]
)
class OAuthAccountEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.STRING)
    val provider: OAuthProviderType,

    @Column(name = "provider_user_id", nullable = false)
    val providerUserId: String,

    @Column(name = "email")
    val email: String?,

    @Column(name = "name")
    val name: String?,

    @Column(name = "avatar_url")
    val avatarUrl: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant
)

/**
 * OAuth provider type for JPA persistence.
 */
enum class OAuthProviderType {
    GOOGLE,
    GITHUB
}
