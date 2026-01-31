package com.aletheia.pros.domain.user

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("User Domain Tests")
class UserTest {

    @Nested
    @DisplayName("User Creation")
    inner class Creation {

        @Test
        fun `should create user with email and password`() {
            // When
            val user = User.create(
                email = "test@example.com",
                passwordHash = "hashed-password",
                name = "Test User"
            )

            // Then
            assertThat(user.email).isEqualTo("test@example.com")
            assertThat(user.passwordHash).isEqualTo("hashed-password")
            assertThat(user.name).isEqualTo("Test User")
            assertThat(user.hasPassword).isTrue()
            assertThat(user.isActive).isTrue()
            assertThat(user.avatarUrl).isNull()
        }

        @Test
        fun `should create user from OAuth without password`() {
            // When
            val user = User.createFromOAuth(
                email = "oauth@example.com",
                name = "OAuth User",
                avatarUrl = "https://example.com/avatar.jpg"
            )

            // Then
            assertThat(user.email).isEqualTo("oauth@example.com")
            assertThat(user.passwordHash).isNull()
            assertThat(user.name).isEqualTo("OAuth User")
            assertThat(user.avatarUrl).isEqualTo("https://example.com/avatar.jpg")
            assertThat(user.hasPassword).isFalse()
            assertThat(user.isActive).isTrue()
        }

        @Test
        fun `should normalize email to lowercase`() {
            // When
            val user = User.create(
                email = "TEST@EXAMPLE.COM",
                passwordHash = "hash",
                name = "Test"
            )

            // Then
            assertThat(user.email).isEqualTo("test@example.com")
        }

        @Test
        fun `should trim name`() {
            // When
            val user = User.create(
                email = "test@example.com",
                passwordHash = "hash",
                name = "  Test User  "
            )

            // Then
            assertThat(user.name).isEqualTo("Test User")
        }
    }

    @Nested
    @DisplayName("User Validation")
    inner class Validation {

        @Test
        fun `should reject blank email`() {
            assertThatThrownBy {
                User.create(
                    email = "   ",
                    passwordHash = "hash",
                    name = "Test"
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Email cannot be blank")
        }

        @Test
        fun `should reject invalid email format`() {
            assertThatThrownBy {
                User.create(
                    email = "invalid-email",
                    passwordHash = "hash",
                    name = "Test"
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Invalid email format")
        }

        @Test
        fun `should reject blank name`() {
            assertThatThrownBy {
                User.create(
                    email = "test@example.com",
                    passwordHash = "hash",
                    name = "   "
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Name cannot be blank")
        }

        @Test
        fun `should reject name exceeding max length`() {
            val longName = "a".repeat(User.MAX_NAME_LENGTH + 1)

            assertThatThrownBy {
                User.create(
                    email = "test@example.com",
                    passwordHash = "hash",
                    name = longName
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Name exceeds maximum length")
        }
    }

    @Nested
    @DisplayName("User Operations")
    inner class Operations {

        @Test
        fun `should record login timestamp`() {
            // Given
            val user = User.create(
                email = "test@example.com",
                passwordHash = "hash",
                name = "Test"
            )
            val loginTime = Instant.now()

            // When
            val updatedUser = user.recordLogin(loginTime)

            // Then
            assertThat(updatedUser.lastLoginAt).isEqualTo(loginTime)
            assertThat(updatedUser.id).isEqualTo(user.id)
        }

        @Test
        fun `should deactivate user`() {
            // Given
            val user = User.create(
                email = "test@example.com",
                passwordHash = "hash",
                name = "Test"
            )

            // When
            val deactivatedUser = user.deactivate()

            // Then
            assertThat(deactivatedUser.isActive).isFalse()
        }

        @Test
        fun `should set password for OAuth user`() {
            // Given
            val user = User.createFromOAuth(
                email = "oauth@example.com",
                name = "OAuth User"
            )
            assertThat(user.hasPassword).isFalse()

            // When
            val updatedUser = user.setPassword("new-password-hash")

            // Then
            assertThat(updatedUser.hasPassword).isTrue()
            assertThat(updatedUser.passwordHash).isEqualTo("new-password-hash")
        }

        @Test
        fun `should update profile`() {
            // Given
            val user = User.create(
                email = "test@example.com",
                passwordHash = "hash",
                name = "Original Name"
            )

            // When
            val updatedUser = user.updateProfile(
                name = "New Name",
                avatarUrl = "https://example.com/new-avatar.jpg"
            )

            // Then
            assertThat(updatedUser.name).isEqualTo("New Name")
            assertThat(updatedUser.avatarUrl).isEqualTo("https://example.com/new-avatar.jpg")
        }

        @Test
        fun `should keep existing values when updating profile with nulls`() {
            // Given
            val user = User(
                id = com.aletheia.pros.domain.common.UserId.generate(),
                email = "test@example.com",
                passwordHash = "hash",
                name = "Original Name",
                avatarUrl = "https://example.com/avatar.jpg",
                createdAt = Instant.now()
            )

            // When
            val updatedUser = user.updateProfile(name = null, avatarUrl = null)

            // Then
            assertThat(updatedUser.name).isEqualTo("Original Name")
            assertThat(updatedUser.avatarUrl).isEqualTo("https://example.com/avatar.jpg")
        }
    }
}
