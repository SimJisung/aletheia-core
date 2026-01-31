package com.aletheia.pros.application.usecase.auth

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.user.OAuthAccount
import com.aletheia.pros.domain.user.OAuthAccountId
import com.aletheia.pros.domain.user.OAuthAccountRepository
import com.aletheia.pros.domain.user.OAuthProvider
import com.aletheia.pros.domain.user.User
import com.aletheia.pros.domain.user.UserRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
@DisplayName("OAuthUseCase Tests")
class OAuthUseCaseTest {

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var oauthAccountRepository: OAuthAccountRepository

    private lateinit var useCase: OAuthUseCase

    private val testUserId = UserId.generate()
    private val testEmail = "test@example.com"
    private val testProviderUserId = "google-12345"

    @BeforeEach
    fun setUp() {
        useCase = OAuthUseCase(
            userRepository = userRepository,
            oauthAccountRepository = oauthAccountRepository
        )
    }

    @Nested
    @DisplayName("Login or Register")
    inner class LoginOrRegister {

        @Test
        fun `should login existing user when OAuth account is linked`() {
            // Given
            val existingUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = null,
                name = "Test User",
                createdAt = Instant.now(),
                isActive = true
            )

            val existingOAuthAccount = OAuthAccount(
                id = OAuthAccountId.generate(),
                userId = testUserId,
                provider = OAuthProvider.GOOGLE,
                providerUserId = testProviderUserId,
                email = testEmail,
                name = "Test User",
                avatarUrl = null,
                createdAt = Instant.now()
            )

            val command = OAuthLoginCommand(
                provider = OAuthProvider.GOOGLE,
                providerUserId = testProviderUserId,
                email = testEmail,
                name = "Test User",
                avatarUrl = null
            )

            every { oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, testProviderUserId) } returns existingOAuthAccount
            every { userRepository.findById(testUserId) } returns existingUser
            every { userRepository.save(any()) } answers { firstArg() }

            // When
            val result = useCase.loginOrRegister(command)

            // Then
            assertThat(result).isInstanceOf(OAuthLoginResult.Success::class.java)
            val success = result as OAuthLoginResult.Success
            assertThat(success.user.email).isEqualTo(testEmail)
            assertThat(success.isNewUser).isFalse()
            assertThat(success.isNewOAuthLink).isFalse()

            verify { userRepository.save(any()) }
        }

        @Test
        fun `should link OAuth and login when user exists by email`() {
            // Given
            val existingUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = "hashed-password",
                name = "Existing User",
                createdAt = Instant.now(),
                isActive = true
            )

            val command = OAuthLoginCommand(
                provider = OAuthProvider.GOOGLE,
                providerUserId = testProviderUserId,
                email = testEmail,
                name = "Google User",
                avatarUrl = "https://example.com/avatar.jpg"
            )

            every { oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, testProviderUserId) } returns null
            every { userRepository.findByEmail(testEmail) } returns existingUser
            every { oauthAccountRepository.save(any()) } answers { firstArg() }
            every { userRepository.save(any()) } answers { firstArg() }

            // When
            val result = useCase.loginOrRegister(command)

            // Then
            assertThat(result).isInstanceOf(OAuthLoginResult.Success::class.java)
            val success = result as OAuthLoginResult.Success
            assertThat(success.user.email).isEqualTo(testEmail)
            assertThat(success.isNewUser).isFalse()
            assertThat(success.isNewOAuthLink).isTrue()

            verify { oauthAccountRepository.save(any()) }
        }

        @Test
        fun `should create new user when no existing user or OAuth link`() {
            // Given
            val command = OAuthLoginCommand(
                provider = OAuthProvider.GITHUB,
                providerUserId = "github-99999",
                email = "newuser@example.com",
                name = "New GitHub User",
                avatarUrl = "https://github.com/avatar.jpg"
            )

            val userSlot = slot<User>()
            val oauthSlot = slot<OAuthAccount>()

            every { oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GITHUB, "github-99999") } returns null
            every { userRepository.findByEmail("newuser@example.com") } returns null
            every { userRepository.save(capture(userSlot)) } answers { firstArg() }
            every { oauthAccountRepository.save(capture(oauthSlot)) } answers { firstArg() }

            // When
            val result = useCase.loginOrRegister(command)

            // Then
            assertThat(result).isInstanceOf(OAuthLoginResult.Success::class.java)
            val success = result as OAuthLoginResult.Success
            assertThat(success.user.email).isEqualTo("newuser@example.com")
            assertThat(success.user.name).isEqualTo("New GitHub User")
            assertThat(success.user.avatarUrl).isEqualTo("https://github.com/avatar.jpg")
            assertThat(success.user.hasPassword).isFalse()
            assertThat(success.isNewUser).isTrue()
            assertThat(success.isNewOAuthLink).isTrue()

            assertThat(oauthSlot.captured.provider).isEqualTo(OAuthProvider.GITHUB)
            assertThat(oauthSlot.captured.providerUserId).isEqualTo("github-99999")
        }

        @Test
        fun `should return EmailRequired when email is null`() {
            // Given
            val command = OAuthLoginCommand(
                provider = OAuthProvider.GITHUB,
                providerUserId = "github-no-email",
                email = null,
                name = "Private Email User",
                avatarUrl = null
            )

            every { oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GITHUB, "github-no-email") } returns null

            // When
            val result = useCase.loginOrRegister(command)

            // Then
            assertThat(result).isEqualTo(OAuthLoginResult.EmailRequired)
        }

        @Test
        fun `should return AccountDeactivated when linked user is inactive`() {
            // Given
            val deactivatedUser = User(
                id = testUserId,
                email = testEmail,
                passwordHash = null,
                name = "Deactivated User",
                createdAt = Instant.now(),
                isActive = false
            )

            val existingOAuthAccount = OAuthAccount(
                id = OAuthAccountId.generate(),
                userId = testUserId,
                provider = OAuthProvider.GOOGLE,
                providerUserId = testProviderUserId,
                email = testEmail,
                name = "Deactivated User",
                avatarUrl = null,
                createdAt = Instant.now()
            )

            val command = OAuthLoginCommand(
                provider = OAuthProvider.GOOGLE,
                providerUserId = testProviderUserId,
                email = testEmail,
                name = "Test User",
                avatarUrl = null
            )

            every { oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, testProviderUserId) } returns existingOAuthAccount
            every { userRepository.findById(testUserId) } returns deactivatedUser

            // When
            val result = useCase.loginOrRegister(command)

            // Then
            assertThat(result).isEqualTo(OAuthLoginResult.AccountDeactivated)
        }

        @Test
        fun `should use email prefix as name when name is null`() {
            // Given
            val command = OAuthLoginCommand(
                provider = OAuthProvider.GOOGLE,
                providerUserId = "google-no-name",
                email = "noname@example.com",
                name = null,
                avatarUrl = null
            )

            val userSlot = slot<User>()

            every { oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-no-name") } returns null
            every { userRepository.findByEmail("noname@example.com") } returns null
            every { userRepository.save(capture(userSlot)) } answers { firstArg() }
            every { oauthAccountRepository.save(any()) } answers { firstArg() }

            // When
            val result = useCase.loginOrRegister(command)

            // Then
            assertThat(result).isInstanceOf(OAuthLoginResult.Success::class.java)
            val success = result as OAuthLoginResult.Success
            assertThat(success.user.name).isEqualTo("noname")
        }
    }

    @Nested
    @DisplayName("Unlink OAuth Account")
    inner class UnlinkAccount {

        @Test
        fun `should unlink OAuth account when user has password`() {
            // Given
            val user = User(
                id = testUserId,
                email = testEmail,
                passwordHash = "hashed-password",
                name = "Test User",
                createdAt = Instant.now(),
                isActive = true
            )

            val oauthAccountId = OAuthAccountId.generate()
            val oauthAccount = OAuthAccount(
                id = oauthAccountId,
                userId = testUserId,
                provider = OAuthProvider.GOOGLE,
                providerUserId = testProviderUserId,
                email = testEmail,
                name = "Test User",
                avatarUrl = null,
                createdAt = Instant.now()
            )

            val command = UnlinkOAuthCommand(
                userId = testUserId,
                oauthAccountId = oauthAccountId
            )

            every { userRepository.findById(testUserId) } returns user
            every { oauthAccountRepository.findByUserId(testUserId) } returns listOf(oauthAccount)
            every { oauthAccountRepository.deleteById(oauthAccountId) } returns Unit

            // When
            val result = useCase.unlinkAccount(command)

            // Then
            assertThat(result).isEqualTo(UnlinkResult.Success)
            verify { oauthAccountRepository.deleteById(oauthAccountId) }
        }

        @Test
        fun `should fail to unlink last OAuth account when user has no password`() {
            // Given
            val user = User(
                id = testUserId,
                email = testEmail,
                passwordHash = null,
                name = "OAuth Only User",
                createdAt = Instant.now(),
                isActive = true
            )

            val oauthAccountId = OAuthAccountId.generate()
            val oauthAccount = OAuthAccount(
                id = oauthAccountId,
                userId = testUserId,
                provider = OAuthProvider.GOOGLE,
                providerUserId = testProviderUserId,
                email = testEmail,
                name = "Test User",
                avatarUrl = null,
                createdAt = Instant.now()
            )

            val command = UnlinkOAuthCommand(
                userId = testUserId,
                oauthAccountId = oauthAccountId
            )

            every { userRepository.findById(testUserId) } returns user
            every { oauthAccountRepository.findByUserId(testUserId) } returns listOf(oauthAccount)

            // When
            val result = useCase.unlinkAccount(command)

            // Then
            assertThat(result).isEqualTo(UnlinkResult.CannotUnlinkLastAuthMethod)
            verify(exactly = 0) { oauthAccountRepository.deleteById(any()) }
        }

        @Test
        fun `should allow unlink when user has multiple OAuth accounts`() {
            // Given
            val user = User(
                id = testUserId,
                email = testEmail,
                passwordHash = null,
                name = "Multi OAuth User",
                createdAt = Instant.now(),
                isActive = true
            )

            val googleOAuthId = OAuthAccountId.generate()
            val githubOAuthId = OAuthAccountId.generate()

            val googleAccount = OAuthAccount(
                id = googleOAuthId,
                userId = testUserId,
                provider = OAuthProvider.GOOGLE,
                providerUserId = "google-123",
                email = testEmail,
                name = "Test User",
                avatarUrl = null,
                createdAt = Instant.now()
            )

            val githubAccount = OAuthAccount(
                id = githubOAuthId,
                userId = testUserId,
                provider = OAuthProvider.GITHUB,
                providerUserId = "github-456",
                email = testEmail,
                name = "Test User",
                avatarUrl = null,
                createdAt = Instant.now()
            )

            val command = UnlinkOAuthCommand(
                userId = testUserId,
                oauthAccountId = googleOAuthId
            )

            every { userRepository.findById(testUserId) } returns user
            every { oauthAccountRepository.findByUserId(testUserId) } returns listOf(googleAccount, githubAccount)
            every { oauthAccountRepository.deleteById(googleOAuthId) } returns Unit

            // When
            val result = useCase.unlinkAccount(command)

            // Then
            assertThat(result).isEqualTo(UnlinkResult.Success)
            verify { oauthAccountRepository.deleteById(googleOAuthId) }
        }

        @Test
        fun `should return UserNotFound when user does not exist`() {
            // Given
            val command = UnlinkOAuthCommand(
                userId = testUserId,
                oauthAccountId = OAuthAccountId.generate()
            )

            every { userRepository.findById(testUserId) } returns null

            // When
            val result = useCase.unlinkAccount(command)

            // Then
            assertThat(result).isEqualTo(UnlinkResult.UserNotFound)
        }

        @Test
        fun `should return AccountNotFound when OAuth account does not exist`() {
            // Given
            val user = User(
                id = testUserId,
                email = testEmail,
                passwordHash = "hashed",
                name = "Test User",
                createdAt = Instant.now(),
                isActive = true
            )

            val nonExistentOAuthId = OAuthAccountId.generate()
            val command = UnlinkOAuthCommand(
                userId = testUserId,
                oauthAccountId = nonExistentOAuthId
            )

            every { userRepository.findById(testUserId) } returns user
            every { oauthAccountRepository.findByUserId(testUserId) } returns emptyList()

            // When
            val result = useCase.unlinkAccount(command)

            // Then
            assertThat(result).isEqualTo(UnlinkResult.AccountNotFound)
        }
    }
}
