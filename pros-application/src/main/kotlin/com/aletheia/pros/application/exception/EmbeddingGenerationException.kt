package com.aletheia.pros.application.exception

/**
 * Exception thrown when embedding generation fails.
 *
 * This can occur due to various reasons such as network issues,
 * API errors, or invalid input.
 */
class EmbeddingGenerationException(
    message: String = "Failed to generate embedding",
    cause: Throwable? = null
) : RuntimeException(message, cause)
