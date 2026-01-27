package com.aletheia.pros.api.dto.response

import com.aletheia.pros.domain.decision.DecisionFeedback
import java.time.Instant

/**
 * Response for decision feedback.
 */
data class FeedbackResponse(
    val id: String,
    val decisionId: String,
    val feedbackType: String,
    val createdAt: Instant
) {
    companion object {
        fun from(feedback: DecisionFeedback): FeedbackResponse {
            return FeedbackResponse(
                id = feedback.id.toString(),
                decisionId = feedback.decisionId.toString(),
                feedbackType = feedback.feedbackType.name,
                createdAt = feedback.createdAt
            )
        }
    }
}
