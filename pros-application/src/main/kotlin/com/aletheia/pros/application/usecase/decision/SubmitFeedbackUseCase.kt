package com.aletheia.pros.application.usecase.decision

import com.aletheia.pros.application.port.input.SubmitFeedbackCommand
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.DecisionFeedback
import com.aletheia.pros.domain.decision.DecisionRepository

/**
 * Use case for submitting feedback on a decision.
 *
 * Feedback is used to improve future projections by adjusting:
 * - Regret sensitivity (lambda)
 * - Value axis weights
 */
class SubmitFeedbackUseCase(
    private val decisionRepository: DecisionRepository
) {

    /**
     * Submits feedback for a decision.
     *
     * @param command The feedback command with decision ID and feedback type
     * @param userId The user ID (for ownership verification)
     * @return The result of the feedback submission
     */
    suspend fun execute(command: SubmitFeedbackCommand, userId: UserId): FeedbackResult {
        // 1. Find decision
        val decision = decisionRepository.findById(command.decisionId)
            ?: return FeedbackResult.NotFound

        // 2. Ownership check
        if (decision.userId != userId) {
            return FeedbackResult.NotFound
        }

        // 3. Check for existing feedback
        if (decisionRepository.hasFeedback(command.decisionId)) {
            return FeedbackResult.AlreadyExists
        }

        // 4. Create and save feedback
        val feedback = DecisionFeedback.create(
            decisionId = command.decisionId,
            feedbackType = command.feedbackType
        )

        val saved = decisionRepository.saveFeedback(feedback)
        return FeedbackResult.Success(saved)
    }
}

/**
 * Result of a feedback submission.
 */
sealed class FeedbackResult {
    /**
     * Feedback was successfully submitted.
     */
    data class Success(val feedback: DecisionFeedback) : FeedbackResult()

    /**
     * Decision was not found or not owned by the user.
     */
    data object NotFound : FeedbackResult()

    /**
     * Feedback already exists for this decision.
     */
    data object AlreadyExists : FeedbackResult()
}
