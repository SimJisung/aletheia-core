package com.aletheia.pros.domain.decision

import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.FeedbackId
import java.time.Instant

/**
 * DecisionFeedback captures user's post-decision satisfaction.
 *
 * This feedback is used to:
 * - Adjust the regret sensitivity parameter (λ)
 * - Update value axis weights
 * - Improve regret risk priors
 *
 * Feedback is collected 24-72 hours after the decision.
 */
data class DecisionFeedback(
    val id: FeedbackId,
    val decisionId: DecisionId,
    val feedbackType: FeedbackType,
    val createdAt: Instant
) {
    companion object {
        /**
         * Creates a new feedback entry.
         */
        fun create(
            decisionId: DecisionId,
            feedbackType: FeedbackType,
            createdAt: Instant = Instant.now()
        ): DecisionFeedback = DecisionFeedback(
            id = FeedbackId.generate(),
            decisionId = decisionId,
            feedbackType = feedbackType,
            createdAt = createdAt
        )
    }
}

/**
 * FeedbackType represents the user's post-decision satisfaction.
 *
 * We use exactly 3 options for simplicity:
 * - Easy for users to provide quickly
 * - Sufficient granularity for learning
 * - No judgment implied (all responses are valid)
 */
enum class FeedbackType(
    val displayNameKo: String,
    val displayNameEn: String,
    val regretSignal: Double
) {
    /**
     * User is satisfied with their decision.
     * No regret signal.
     */
    SATISFIED(
        displayNameKo = "만족",
        displayNameEn = "Satisfied",
        regretSignal = 0.0
    ),

    /**
     * User has neutral feelings about their decision.
     * Mild uncertainty signal.
     */
    NEUTRAL(
        displayNameKo = "보통",
        displayNameEn = "Neutral",
        regretSignal = 0.3
    ),

    /**
     * User regrets their decision.
     * Strong regret signal for learning.
     */
    REGRET(
        displayNameKo = "후회",
        displayNameEn = "Regret",
        regretSignal = 1.0
    )
}
