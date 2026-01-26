package com.aletheia.pros.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request to create a new decision projection.
 */
data class CreateDecisionRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 500, message = "Title must not exceed 500 characters")
    val title: String,

    @field:NotBlank(message = "Option A is required")
    @field:Size(max = 2000, message = "Option A must not exceed 2000 characters")
    val optionA: String,

    @field:NotBlank(message = "Option B is required")
    @field:Size(max = 2000, message = "Option B must not exceed 2000 characters")
    val optionB: String,

    /**
     * Optional priority value axis.
     * Valid values: GROWTH, STABILITY, FINANCIAL, AUTONOMY, RELATIONSHIP, ACHIEVEMENT, HEALTH, MEANING
     */
    val priorityAxis: String? = null
)

/**
 * Query parameters for listing decisions.
 */
data class ListDecisionsParams(
    val limit: Int = 20,
    val offset: Int = 0
) {
    init {
        require(limit in 1..100) { "Limit must be between 1 and 100" }
        require(offset >= 0) { "Offset must be non-negative" }
    }
}
