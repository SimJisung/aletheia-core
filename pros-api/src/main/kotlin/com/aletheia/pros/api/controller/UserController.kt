package com.aletheia.pros.api.controller

import com.aletheia.pros.api.dto.response.UserResponse
import com.aletheia.pros.api.security.SecurityUtils
import com.aletheia.pros.application.usecase.user.GetCurrentUserUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST Controller for user operations.
 */
@RestController
@RequestMapping("/v1/users")
@Tag(name = "Users", description = "User profile management")
class UserController(
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) {

    /**
     * Gets the current authenticated user's information.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user information")
    fun getCurrentUser(): ResponseEntity<UserResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val user = getCurrentUserUseCase.execute(userId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(UserResponse.from(user))
    }
}
