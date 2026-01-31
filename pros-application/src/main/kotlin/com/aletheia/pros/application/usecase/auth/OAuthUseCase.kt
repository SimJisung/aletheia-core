package com.aletheia.pros.application.usecase.auth

import com.aletheia.pros.domain.user.OAuthAccount
import com.aletheia.pros.domain.user.OAuthAccountRepository
import com.aletheia.pros.domain.user.OAuthProvider
import com.aletheia.pros.domain.user.User
import com.aletheia.pros.domain.user.UserRepository

/**
 * Use case for OAuth authentication operations.
 */
class OAuthUseCase(
    private val userRepository: UserRepository,
    private val oauthAccountRepository: OAuthAccountRepository
) {

    /**
     * Authenticates or registers a user via OAuth.
     *
     * Flow:
     * 1. Check if OAuth account already exists -> login existing user
     * 2. Check if email already exists -> link OAuth to existing user
     * 3. Otherwise -> create new user with OAuth link
     *
     * @return OAuthLoginResult with user or error
     */
    fun loginOrRegister(command: OAuthLoginCommand): OAuthLoginResult {
        // 1. Check if OAuth account already linked
        val existingOAuthAccount = oauthAccountRepository.findByProviderAndProviderUserId(
            command.provider,
            command.providerUserId
        )

        if (existingOAuthAccount != null) {
            // Existing OAuth link found - login
            val user = userRepository.findById(existingOAuthAccount.userId)
                ?: return OAuthLoginResult.UserNotFound

            if (!user.isActive) {
                return OAuthLoginResult.AccountDeactivated
            }

            val updatedUser = user.recordLogin()
            userRepository.save(updatedUser)

            return OAuthLoginResult.Success(
                user = updatedUser,
                isNewUser = false,
                isNewOAuthLink = false
            )
        }

        // 2. Check if user with this email already exists
        val email = command.email
            ?: return OAuthLoginResult.EmailRequired

        val existingUser = userRepository.findByEmail(email)

        if (existingUser != null) {
            // User exists - link OAuth account
            if (!existingUser.isActive) {
                return OAuthLoginResult.AccountDeactivated
            }

            val oauthAccount = OAuthAccount.create(
                userId = existingUser.id,
                provider = command.provider,
                providerUserId = command.providerUserId,
                email = command.email,
                name = command.name,
                avatarUrl = command.avatarUrl
            )
            oauthAccountRepository.save(oauthAccount)

            // Update user's avatar if not set
            val updatedUser = if (existingUser.avatarUrl == null && command.avatarUrl != null) {
                existingUser.updateProfile(avatarUrl = command.avatarUrl).recordLogin()
            } else {
                existingUser.recordLogin()
            }
            userRepository.save(updatedUser)

            return OAuthLoginResult.Success(
                user = updatedUser,
                isNewUser = false,
                isNewOAuthLink = true
            )
        }

        // 3. Create new user with OAuth
        val name = command.name
            ?: email.substringBefore("@")

        val newUser = User.createFromOAuth(
            email = email,
            name = name,
            avatarUrl = command.avatarUrl
        )
        val savedUser = userRepository.save(newUser)

        val oauthAccount = OAuthAccount.create(
            userId = savedUser.id,
            provider = command.provider,
            providerUserId = command.providerUserId,
            email = command.email,
            name = command.name,
            avatarUrl = command.avatarUrl
        )
        oauthAccountRepository.save(oauthAccount)

        return OAuthLoginResult.Success(
            user = savedUser,
            isNewUser = true,
            isNewOAuthLink = true
        )
    }

    /**
     * Gets all linked OAuth accounts for a user.
     */
    fun getLinkedAccounts(userId: com.aletheia.pros.domain.common.UserId): List<OAuthAccount> {
        return oauthAccountRepository.findByUserId(userId)
    }

    /**
     * Unlinks an OAuth account from a user.
     * Fails if it's the only authentication method (no password and only one OAuth link).
     */
    fun unlinkAccount(command: UnlinkOAuthCommand): UnlinkResult {
        val user = userRepository.findById(command.userId)
            ?: return UnlinkResult.UserNotFound

        val linkedAccounts = oauthAccountRepository.findByUserId(command.userId)
        val accountToUnlink = linkedAccounts.find { it.id == command.oauthAccountId }
            ?: return UnlinkResult.AccountNotFound

        // Check if this is the only auth method
        if (!user.hasPassword && linkedAccounts.size <= 1) {
            return UnlinkResult.CannotUnlinkLastAuthMethod
        }

        oauthAccountRepository.deleteById(command.oauthAccountId)
        return UnlinkResult.Success
    }
}

/**
 * Command for OAuth login/registration.
 */
data class OAuthLoginCommand(
    val provider: OAuthProvider,
    val providerUserId: String,
    val email: String?,
    val name: String?,
    val avatarUrl: String?
) {
    init {
        require(providerUserId.isNotBlank()) { "Provider user ID cannot be blank" }
    }
}

/**
 * Command for unlinking OAuth account.
 */
data class UnlinkOAuthCommand(
    val userId: com.aletheia.pros.domain.common.UserId,
    val oauthAccountId: com.aletheia.pros.domain.user.OAuthAccountId
)

/**
 * Result of OAuth login/registration operation.
 */
sealed class OAuthLoginResult {
    data class Success(
        val user: User,
        val isNewUser: Boolean,
        val isNewOAuthLink: Boolean
    ) : OAuthLoginResult()

    data object EmailRequired : OAuthLoginResult()
    data object UserNotFound : OAuthLoginResult()
    data object AccountDeactivated : OAuthLoginResult()
}

/**
 * Result of OAuth unlink operation.
 */
sealed class UnlinkResult {
    data object Success : UnlinkResult()
    data object UserNotFound : UnlinkResult()
    data object AccountNotFound : UnlinkResult()
    data object CannotUnlinkLastAuthMethod : UnlinkResult()
}
