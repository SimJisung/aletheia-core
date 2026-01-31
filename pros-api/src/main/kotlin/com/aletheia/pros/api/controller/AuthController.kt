package com.aletheia.pros.api.controller

import com.aletheia.pros.api.dto.request.LoginRequest
import com.aletheia.pros.api.dto.request.RegisterRequest
import com.aletheia.pros.api.dto.response.AuthResponse
import com.aletheia.pros.api.dto.response.UserResponse
import com.aletheia.pros.api.security.JwtTokenProvider
import com.aletheia.pros.application.usecase.auth.AuthUseCase
import com.aletheia.pros.application.usecase.auth.LoginCommand
import com.aletheia.pros.application.usecase.auth.LoginResult
import com.aletheia.pros.application.usecase.auth.RegisterCommand
import com.aletheia.pros.application.usecase.auth.RegisterResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for authentication operations.
 */
@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "User authentication (register/login)")
class AuthController(
    private val authUseCase: AuthUseCase,
    private val jwtTokenProvider: JwtTokenProvider
) {

    /**
     * Registers a new user account.
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    fun register(
        @Valid @RequestBody request: RegisterRequest
    ): ResponseEntity<AuthResponse> {
        val command = RegisterCommand(
            email = request.email,
            password = request.password,
            name = request.name
        )

        return when (val result = authUseCase.register(command)) {
            is RegisterResult.Success -> {
                val token = jwtTokenProvider.generateToken(result.user.id, result.user.email)
                ResponseEntity.status(HttpStatus.CREATED).body(
                    AuthResponse(
                        token = token,
                        user = UserResponse.from(result.user)
                    )
                )
            }
            is RegisterResult.EmailAlreadyExists -> {
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            }
        }
    }

    /**
     * Authenticates a user and returns a JWT token.
     */
    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token")
    fun login(
        @Valid @RequestBody request: LoginRequest
    ): ResponseEntity<AuthResponse> {
        val command = LoginCommand(
            email = request.email,
            password = request.password
        )

        return when (val result = authUseCase.login(command)) {
            is LoginResult.Success -> {
                val token = jwtTokenProvider.generateToken(result.user.id, result.user.email)
                ResponseEntity.ok(
                    AuthResponse(
                        token = token,
                        user = UserResponse.from(result.user)
                    )
                )
            }
            is LoginResult.InvalidCredentials -> {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            }
            is LoginResult.AccountDeactivated -> {
                ResponseEntity.status(HttpStatus.FORBIDDEN).build()
            }
            is LoginResult.OAuthOnlyAccount -> {
                // User registered via OAuth and has no password
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            }
        }
    }
}
