package com.aletheia.pros.application.exception

/**
 * Exception thrown when OpenAI API quota is exceeded.
 *
 * This is a non-retryable exception that should be handled at the API layer
 * to return an appropriate HTTP status (402 or 429).
 */
class QuotaExceededException(
    message: String = "OpenAI API quota exceeded. Please check your plan and billing details.",
    cause: Throwable? = null
) : RuntimeException(message, cause)
