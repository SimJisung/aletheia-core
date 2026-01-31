package com.aletheia.pros.domain.user

/**
 * Supported OAuth providers for social login.
 */
enum class OAuthProvider {
    GOOGLE,
    GITHUB;

    companion object {
        fun fromRegistrationId(registrationId: String): OAuthProvider {
            return when (registrationId.lowercase()) {
                "google" -> GOOGLE
                "github" -> GITHUB
                else -> throw IllegalArgumentException("Unsupported OAuth provider: $registrationId")
            }
        }
    }
}
