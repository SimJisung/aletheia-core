package com.aletheia.pros.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * Request to submit feedback on a decision.
 */
data class SubmitFeedbackRequest(
    @field:NotBlank(message = "Feedback type is required")
    @field:Pattern(
        regexp = "SATISFIED|NEUTRAL|REGRET",
        message = "Feedback type must be one of: SATISFIED, NEUTRAL, REGRET"
    )
    val feedbackType: String
)
