package com.aletheia.pros.api.dto.response

import com.aletheia.pros.application.usecase.decision.FeedbackImpact
import com.aletheia.pros.application.usecase.decision.ParameterUpdate
import com.aletheia.pros.domain.decision.DecisionFeedback
import com.aletheia.pros.domain.decision.FeedbackStats
import java.time.Instant

/**
 * Response for decision feedback.
 */
data class FeedbackResponse(
    val id: String,
    val decisionId: String,
    val feedbackType: String,
    val createdAt: Instant,
    /** Impact information showing how the feedback affects future calculations */
    val impact: FeedbackImpactResponse
) {
    companion object {
        fun from(feedback: DecisionFeedback, impact: FeedbackImpact): FeedbackResponse {
            return FeedbackResponse(
                id = feedback.id.toString(),
                decisionId = feedback.decisionId.toString(),
                feedbackType = feedback.feedbackType.name,
                createdAt = feedback.createdAt,
                impact = FeedbackImpactResponse.from(impact)
            )
        }
    }
}

/**
 * Response for feedback impact information.
 *
 * Shows how the submitted feedback affects future decision calculations.
 */
data class FeedbackImpactResponse(
    /** Cumulative feedback statistics for the user */
    val stats: FeedbackStatsResponse,
    /** Parameter update information (null if no update occurred) */
    val parameterUpdate: ParameterUpdateResponse?,
    /** User-friendly description of the feedback effect */
    val effectDescription: String
) {
    companion object {
        fun from(impact: FeedbackImpact): FeedbackImpactResponse {
            return FeedbackImpactResponse(
                stats = FeedbackStatsResponse.from(impact.stats),
                parameterUpdate = impact.parameterUpdate?.let { ParameterUpdateResponse.from(it) },
                effectDescription = impact.effectDescription
            )
        }
    }
}

/**
 * Response for feedback statistics.
 */
data class FeedbackStatsResponse(
    /** Total number of decisions made by the user */
    val totalDecisions: Int,
    /** Number of decisions with feedback */
    val totalWithFeedback: Int,
    /** Number of satisfied feedback responses */
    val satisfiedCount: Int,
    /** Number of neutral feedback responses */
    val neutralCount: Int,
    /** Number of regret feedback responses */
    val regretCount: Int,
    /** Overall regret rate (0.0 to 1.0) */
    val regretRate: Double
) {
    companion object {
        fun from(stats: FeedbackStats): FeedbackStatsResponse {
            return FeedbackStatsResponse(
                totalDecisions = stats.totalDecisions,
                totalWithFeedback = stats.totalWithFeedback,
                satisfiedCount = stats.satisfiedCount,
                neutralCount = stats.neutralCount,
                regretCount = stats.regretCount,
                regretRate = stats.regretRate
            )
        }
    }
}

/**
 * Response for parameter update information.
 *
 * Shows the before/after values of parameters adjusted by the lambda learning algorithm.
 */
data class ParameterUpdateResponse(
    /** Lambda (regret sensitivity) value before the feedback */
    val lambdaBefore: Double,
    /** Lambda value after the feedback */
    val lambdaAfter: Double,
    /** Regret prior value before the feedback */
    val regretPriorBefore: Double,
    /** Regret prior value after the feedback */
    val regretPriorAfter: Double,
    /** Human-readable reason for the update */
    val reason: String
) {
    companion object {
        fun from(update: ParameterUpdate): ParameterUpdateResponse {
            return ParameterUpdateResponse(
                lambdaBefore = update.lambdaBefore,
                lambdaAfter = update.lambdaAfter,
                regretPriorBefore = update.regretPriorBefore,
                regretPriorAfter = update.regretPriorAfter,
                reason = update.reason
            )
        }
    }
}
