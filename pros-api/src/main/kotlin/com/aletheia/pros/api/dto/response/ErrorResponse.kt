package com.aletheia.pros.api.dto.response

import java.time.Instant

/**
 * Standard error response.
 */
data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String
)

/**
 * Validation error response with field-level errors.
 */
data class ValidationErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int = 400,
    val error: String = "Bad Request",
    val message: String = "Validation failed",
    val path: String,
    val errors: List<FieldError>
)

/**
 * Individual field validation error.
 */
data class FieldError(
    val field: String,
    val message: String
)
