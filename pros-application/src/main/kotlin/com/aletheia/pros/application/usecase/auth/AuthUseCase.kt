package com.aletheia.pros.application.usecase.auth

import com.aletheia.pros.domain.user.User
import com.aletheia.pros.domain.user.UserRepository

/**
 * Use case for user authentication operations.
 */
class AuthUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoderPort
) {

    /**
     * Registers a new user.
     *
     * @return RegisterResult with the created user or error
     */
    fun register(command: RegisterCommand): RegisterResult {
        // Check if email already exists
        if (userRepository.existsByEmail(command.email)) {
            return RegisterResult.EmailAlreadyExists
        }

        // Create user with hashed password
        val passwordHash = passwordEncoder.encode(command.password)
        val user = User.create(
            email = command.email,
            passwordHash = passwordHash,
            name = command.name
        )

        val savedUser = userRepository.save(user)
        return RegisterResult.Success(savedUser)
    }

    /**
     * Authenticates a user with email and password.
     *
     * @return LoginResult with user or error
     */
    fun login(command: LoginCommand): LoginResult {
        val user = userRepository.findByEmail(command.email)
            ?: return LoginResult.InvalidCredentials

        if (!user.isActive) {
            return LoginResult.AccountDeactivated
        }

        // Check if user has a password (OAuth-only users don't)
        if (!user.hasPassword) {
            return LoginResult.OAuthOnlyAccount
        }

        if (!passwordEncoder.matches(command.password, user.passwordHash!!)) {
            return LoginResult.InvalidCredentials
        }

        // Update last login time
        val updatedUser = user.recordLogin()
        userRepository.save(updatedUser)

        return LoginResult.Success(updatedUser)
    }
}

/**
 * Command for user registration.
 */
data class RegisterCommand(
    val email: String,
    val password: String,
    val name: String
) {
    init {
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(password.length >= 8) { "Password must be at least 8 characters" }
        require(name.isNotBlank()) { "Name cannot be blank" }
    }
}

/**
 * Command for user login.
 */
data class LoginCommand(
    val email: String,
    val password: String
)

/**
 * Result of registration operation.
 */
sealed class RegisterResult {
    data class Success(val user: User) : RegisterResult()
    data object EmailAlreadyExists : RegisterResult()
}

/**
 * Result of login operation.
 */
sealed class LoginResult {
    data class Success(val user: User) : LoginResult()
    data object InvalidCredentials : LoginResult()
    data object AccountDeactivated : LoginResult()
    data object OAuthOnlyAccount : LoginResult()
}

/**
 * Port for password encoding operations.
 */
interface PasswordEncoderPort {
    fun encode(rawPassword: String): String
    fun matches(rawPassword: String, encodedPassword: String): Boolean
}
