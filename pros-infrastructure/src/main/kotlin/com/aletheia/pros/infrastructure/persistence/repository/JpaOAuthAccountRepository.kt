package com.aletheia.pros.infrastructure.persistence.repository

import com.aletheia.pros.infrastructure.persistence.entity.OAuthAccountEntity
import com.aletheia.pros.infrastructure.persistence.entity.OAuthProviderType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * JPA Repository for OAuth accounts.
 */
@Repository
interface JpaOAuthAccountRepository : JpaRepository<OAuthAccountEntity, UUID> {

    fun findByProviderAndProviderUserId(provider: OAuthProviderType, providerUserId: String): OAuthAccountEntity?

    fun findByUserId(userId: UUID): List<OAuthAccountEntity>

    fun existsByProviderAndProviderUserId(provider: OAuthProviderType, providerUserId: String): Boolean
}
