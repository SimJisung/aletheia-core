package com.aletheia.pros.infrastructure.persistence.mapper

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.user.OAuthAccount
import com.aletheia.pros.domain.user.OAuthAccountId
import com.aletheia.pros.domain.user.OAuthProvider
import com.aletheia.pros.infrastructure.persistence.entity.OAuthAccountEntity
import com.aletheia.pros.infrastructure.persistence.entity.OAuthProviderType
import org.springframework.stereotype.Component

/**
 * Mapper for OAuthAccount domain <-> entity conversion.
 */
@Component
class OAuthAccountMapper {

    fun toEntity(domain: OAuthAccount): OAuthAccountEntity {
        return OAuthAccountEntity(
            id = domain.id.value,
            userId = domain.userId.value,
            provider = toProviderType(domain.provider),
            providerUserId = domain.providerUserId,
            email = domain.email,
            name = domain.name,
            avatarUrl = domain.avatarUrl,
            createdAt = domain.createdAt
        )
    }

    fun toDomain(entity: OAuthAccountEntity): OAuthAccount {
        return OAuthAccount(
            id = OAuthAccountId(entity.id),
            userId = UserId(entity.userId),
            provider = toProvider(entity.provider),
            providerUserId = entity.providerUserId,
            email = entity.email,
            name = entity.name,
            avatarUrl = entity.avatarUrl,
            createdAt = entity.createdAt
        )
    }

    private fun toProviderType(provider: OAuthProvider): OAuthProviderType {
        return when (provider) {
            OAuthProvider.GOOGLE -> OAuthProviderType.GOOGLE
            OAuthProvider.GITHUB -> OAuthProviderType.GITHUB
        }
    }

    private fun toProvider(type: OAuthProviderType): OAuthProvider {
        return when (type) {
            OAuthProviderType.GOOGLE -> OAuthProvider.GOOGLE
            OAuthProviderType.GITHUB -> OAuthProvider.GITHUB
        }
    }
}
