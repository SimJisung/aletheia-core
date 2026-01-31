package com.aletheia.pros.domain.user

import com.aletheia.pros.domain.common.UserId

/**
 * Repository interface for OAuth account persistence.
 */
interface OAuthAccountRepository {

    /**
     * Saves an OAuth account.
     */
    fun save(oauthAccount: OAuthAccount): OAuthAccount

    /**
     * Finds an OAuth account by provider and provider user ID.
     */
    fun findByProviderAndProviderUserId(provider: OAuthProvider, providerUserId: String): OAuthAccount?

    /**
     * Finds all OAuth accounts for a user.
     */
    fun findByUserId(userId: UserId): List<OAuthAccount>

    /**
     * Checks if an OAuth account exists for the given provider and provider user ID.
     */
    fun existsByProviderAndProviderUserId(provider: OAuthProvider, providerUserId: String): Boolean

    /**
     * Deletes an OAuth account link.
     */
    fun deleteById(id: OAuthAccountId)
}
