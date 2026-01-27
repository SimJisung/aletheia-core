package com.aletheia.pros.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request to create a new thought fragment.
 */
data class CreateFragmentRequest(
    @field:NotBlank(message = "Text is required")
    @field:Size(max = 10000, message = "Text must not exceed 10000 characters")
    val text: String,

    @field:Size(max = 255, message = "Topic hint must not exceed 255 characters")
    val topicHint: String? = null
)

/**
 * Query parameters for listing fragments.
 */
data class ListFragmentsParams(
    val limit: Int = 20,
    val offset: Int = 0
) {
    init {
        require(limit in 1..100) { "Limit must be between 1 and 100" }
        require(offset >= 0) { "Offset must be non-negative" }
    }
}

/**
 * Request to search for similar fragments.
 */
data class SimilarFragmentsRequest(
    @field:NotBlank(message = "Query text is required")
    val queryText: String,

    val topK: Int = 10
) {
    init {
        require(topK in 1..50) { "topK must be between 1 and 50" }
    }
}
