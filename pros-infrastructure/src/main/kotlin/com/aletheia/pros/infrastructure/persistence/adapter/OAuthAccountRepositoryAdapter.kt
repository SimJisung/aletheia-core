package com.aletheia.pros.infrastructure.persistence.adapter

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.user.OAuthAccount
import com.aletheia.pros.domain.user.OAuthAccountId
import com.aletheia.pros.domain.user.OAuthAccountRepository
import com.aletheia.pros.domain.user.OAuthProvider
import com.aletheia.pros.infrastructure.persistence.entity.OAuthProviderType
import com.aletheia.pros.infrastructure.persistence.mapper.OAuthAccountMapper
import com.aletheia.pros.infrastructure.persistence.repository.JpaOAuthAccountRepository
import org.springframework.stereotype.Repository

/**
 * Adapter implementing OAuthAccountRepository using JPA.
 */
@Repository
class OAuthAccountRepositoryAdapter(
    private val jpaRepository: JpaOAuthAccountRepository,
    private val mapper: OAuthAccountMapper
) : OAuthAccountRepository {

    override fun save(oauthAccount: OAuthAccount): OAuthAccount {
        val entity = mapper.toEntity(oauthAccount)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
    }

    override fun findByProviderAndProviderUserId(provider: OAuthProvider, providerUserId: String): OAuthAccount? {
        val providerType = toProviderType(provider)
        return jpaRepository.findByProviderAndProviderUserId(providerType, providerUserId)
            ?.let { mapper.toDomain(it) }
    }

    override fun findByUserId(userId: UserId): List<OAuthAccount> {
        return jpaRepository.findByUserId(userId.value)
            .map { mapper.toDomain(it) }
    }

    override fun existsByProviderAndProviderUserId(provider: OAuthProvider, providerUserId: String): Boolean {
        val providerType = toProviderType(provider)
        return jpaRepository.existsByProviderAndProviderUserId(providerType, providerUserId)
    }

    override fun deleteById(id: OAuthAccountId) {
        jpaRepository.deleteById(id.value)
    }

    private fun toProviderType(provider: OAuthProvider): OAuthProviderType {
        return when (provider) {
            OAuthProvider.GOOGLE -> OAuthProviderType.GOOGLE
            OAuthProvider.GITHUB -> OAuthProviderType.GITHUB
        }
    }
}
