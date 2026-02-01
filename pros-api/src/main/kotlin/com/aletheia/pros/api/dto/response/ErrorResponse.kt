package com.aletheia.pros.api.dto.response

import com.aletheia.pros.api.util.CorrelationIdHolder
import java.time.Instant

/**
 * Standard error response with correlation ID for request tracing.
 */
data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val correlationId: String? = CorrelationIdHolder.get()
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
    val errors: List<FieldError>,
    val correlationId: String? = CorrelationIdHolder.get()
)

/**
 * Individual field validation error.
 */
data class FieldError(
    val field: String,
    val message: String
)
