package com.aletheia.pros.api.controller

import com.aletheia.pros.api.dto.request.SetValueImportanceRequest
import com.aletheia.pros.api.dto.response.ValueImportanceResponse
import com.aletheia.pros.api.security.SecurityUtils
import com.aletheia.pros.application.usecase.value.GetValueImportanceUseCase
import com.aletheia.pros.application.usecase.value.SetValueImportanceCommand
import com.aletheia.pros.application.usecase.value.SetValueImportanceUseCase
import com.aletheia.pros.domain.value.ValueAxis
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for managing user's value importance settings.
 *
 * These settings allow users to explicitly specify how important each
 * value axis is to them, which influences the Decision valueAlignment calculation.
 */
@RestController
@RequestMapping("/v1/values/importance")
@Tag(name = "Value Importance", description = "User value importance management")
class ValueImportanceController(
    private val setValueImportanceUseCase: SetValueImportanceUseCase,
    private val getValueImportanceUseCase: GetValueImportanceUseCase
) {

    /**
     * Gets the current user's value importance settings.
     *
     * Returns all 8 value axes with their importance scores (1-10 scale).
     * If the user hasn't set any importance values, defaults are returned.
     */
    @GetMapping
    @Operation(
        summary = "Get current user's value importance ratings",
        description = "Returns importance ratings for all 8 value axes. Unset values return default (5.5)."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successfully retrieved importance settings"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun getImportance(): ResponseEntity<ValueImportanceResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val importance = getValueImportanceUseCase.execute(userId)
        return ResponseEntity.ok(ValueImportanceResponse.from(importance))
    }

    /**
     * Sets or updates value importance ratings.
     *
     * Partial updates are supported - you can update only specific axes.
     * Values are merged with existing settings.
     */
    @PutMapping
    @Operation(
        summary = "Set value importance ratings",
        description = "Sets importance ratings for specified value axes (1-10 scale). " +
                "Partial updates supported - unspecified axes keep their current values."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Successfully updated importance settings"),
        ApiResponse(responseCode = "400", description = "Invalid request - invalid axis name or value out of range"),
        ApiResponse(responseCode = "401", description = "Unauthorized")
    )
    fun setImportance(
        @Valid @RequestBody request: SetValueImportanceRequest
    ): ResponseEntity<ValueImportanceResponse> {
        val userId = SecurityUtils.getCurrentUserId()

        // Convert string axis names to ValueAxis enum
        val importanceMap = request.importance.mapNotNull { (key, value) ->
            ValueAxis.fromName(key)?.let { axis -> axis to value }
        }.toMap()

        // Validate that at least one valid axis was provided
        if (importanceMap.isEmpty()) {
            return ResponseEntity.badRequest().build()
        }

        val command = SetValueImportanceCommand(
            userId = userId,
            importanceValues = importanceMap
        )

        val result = setValueImportanceUseCase.execute(command)
        return ResponseEntity.ok(ValueImportanceResponse.from(result))
    }
}
