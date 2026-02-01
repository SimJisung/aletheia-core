package com.aletheia.pros.application.usecase.decision

import com.aletheia.pros.application.port.input.SubmitFeedbackCommand
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.DecisionFeedback
import com.aletheia.pros.domain.decision.DecisionRepository
import com.aletheia.pros.domain.decision.FeedbackStats

/**
 * Use case for submitting feedback on a decision.
 *
 * Feedback is used to improve future projections by adjusting:
 * - Regret sensitivity (lambda)
 * - Value axis weights
 *
 * Lambda Auto-Learning:
 * - If regret rate >= 40% and lambda < 2.0: increase lambda by 10% (more risk-averse)
 * - If regret rate <= 10% and lambda > 0.5: decrease lambda by 5% (prioritize fit)
 */
class SubmitFeedbackUseCase(
    private val decisionRepository: DecisionRepository,
    private val userSettingsProvider: UserSettingsProvider
) {

    companion object {
        private const val HIGH_REGRET_THRESHOLD = 0.4
        private const val LOW_REGRET_THRESHOLD = 0.1
        private const val LAMBDA_INCREASE_FACTOR = 1.1
        private const val LAMBDA_DECREASE_FACTOR = 0.95
        private const val LAMBDA_MAX = 2.0
        private const val LAMBDA_MIN = 0.5
    }

    /**
     * Submits feedback for a decision.
     *
     * @param command The feedback command with decision ID and feedback type
     * @param userId The user ID (for ownership verification)
     * @return The result of the feedback submission with impact information
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

        // 5. Get updated feedback stats
        val stats = decisionRepository.getFeedbackStats(userId)

        // 6. Get current user settings
        val currentSettings = userSettingsProvider.getSettings(userId)

        // 7. Apply lambda learning
        val parameterUpdate = applyLambdaLearning(
            userId = userId,
            stats = stats,
            currentLambda = currentSettings.lambda,
            currentRegretPrior = currentSettings.regretPrior
        )

        // 8. Generate effect description
        val effectDescription = generateEffectDescription(stats, parameterUpdate)

        // 9. Build impact response
        val impact = FeedbackImpact(
            stats = stats,
            parameterUpdate = parameterUpdate,
            effectDescription = effectDescription
        )

        return FeedbackResult.Success(saved, impact)
    }

    /**
     * Applies lambda auto-learning based on feedback patterns.
     *
     * Learning rules (based on score formula: score = fit - λ × regret):
     * - Higher lambda = more weight on regret risk (risk-averse)
     * - Lower lambda = more weight on fit (risk-tolerant)
     */
    private suspend fun applyLambdaLearning(
        userId: UserId,
        stats: FeedbackStats,
        currentLambda: Double,
        currentRegretPrior: Double
    ): ParameterUpdate? {
        val regretRate = stats.regretRate

        val (newLambda, reason) = when {
            regretRate >= HIGH_REGRET_THRESHOLD && currentLambda < LAMBDA_MAX -> {
                val updated = (currentLambda * LAMBDA_INCREASE_FACTOR).coerceAtMost(LAMBDA_MAX)
                updated to "높은 후회율(${(regretRate * 100).toInt()}%)로 인해 리스크 민감도 증가"
            }
            regretRate <= LOW_REGRET_THRESHOLD && currentLambda > LAMBDA_MIN -> {
                val updated = (currentLambda * LAMBDA_DECREASE_FACTOR).coerceAtLeast(LAMBDA_MIN)
                updated to "낮은 후회율(${(regretRate * 100).toInt()}%)로 인해 적합도 중시로 조정"
            }
            else -> {
                return ParameterUpdate(
                    lambdaBefore = currentLambda,
                    lambdaAfter = currentLambda,
                    regretPriorBefore = currentRegretPrior,
                    regretPriorAfter = currentRegretPrior,
                    reason = "현재 후회율(${(regretRate * 100).toInt()}%)은 조정 임계값 미만"
                )
            }
        }

        // Apply the update
        if (newLambda != currentLambda) {
            userSettingsProvider.updateLambda(userId, newLambda)
        }

        return ParameterUpdate(
            lambdaBefore = currentLambda,
            lambdaAfter = newLambda,
            regretPriorBefore = currentRegretPrior,
            regretPriorAfter = currentRegretPrior,
            reason = reason
        )
    }

    private fun generateEffectDescription(stats: FeedbackStats, parameterUpdate: ParameterUpdate?): String {
        val baseMessage = "피드백이 저장되었습니다. 향후 결정 분석 시 후회 리스크 계산에 반영됩니다."

        return if (parameterUpdate != null && parameterUpdate.lambdaBefore != parameterUpdate.lambdaAfter) {
            "$baseMessage Lambda가 ${String.format("%.2f", parameterUpdate.lambdaBefore)}에서 " +
                    "${String.format("%.2f", parameterUpdate.lambdaAfter)}(으)로 조정되었습니다."
        } else {
            baseMessage
        }
    }
}

/**
 * Result of a feedback submission.
 */
sealed class FeedbackResult {
    /**
     * Feedback was successfully submitted.
     */
    data class Success(
        val feedback: DecisionFeedback,
        val impact: FeedbackImpact
    ) : FeedbackResult()

    /**
     * Decision was not found or not owned by the user.
     */
    data object NotFound : FeedbackResult()

    /**
     * Feedback already exists for this decision.
     */
    data object AlreadyExists : FeedbackResult()
}

/**
 * Impact information about feedback submission.
 *
 * Provides transparency about how feedback affects future calculations.
 */
data class FeedbackImpact(
    /** Cumulative feedback statistics for the user */
    val stats: FeedbackStats,
    /** Parameter update information (null if no update needed) */
    val parameterUpdate: ParameterUpdate?,
    /** User-friendly description of the feedback effect */
    val effectDescription: String
)

/**
 * Information about parameter updates due to feedback learning.
 */
data class ParameterUpdate(
    /** Lambda value before this feedback */
    val lambdaBefore: Double,
    /** Lambda value after this feedback */
    val lambdaAfter: Double,
    /** Regret prior before this feedback */
    val regretPriorBefore: Double,
    /** Regret prior after this feedback */
    val regretPriorAfter: Double,
    /** Human-readable reason for the update */
    val reason: String
)
