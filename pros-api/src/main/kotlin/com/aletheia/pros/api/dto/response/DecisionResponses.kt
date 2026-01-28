package com.aletheia.pros.api.dto.response

import com.aletheia.pros.application.port.input.DecisionExplanation
import com.aletheia.pros.domain.decision.Decision
import java.time.Instant

/**
 * Response for a decision projection.
 *
 * NOTE: This is NOT a recommendation. The probabilities reflect
 * how well each option fits the user's historical patterns.
 */
data class DecisionResponse(
    val id: String,
    val title: String,
    val optionA: String,
    val optionB: String,
    val priorityAxis: String?,
    val result: DecisionResultResponse,
    val createdAt: Instant
) {
    companion object {
        fun from(decision: Decision): DecisionResponse {
            return DecisionResponse(
                id = decision.id.toString(),
                title = decision.title,
                optionA = decision.optionA,
                optionB = decision.optionB,
                priorityAxis = decision.priorityAxis?.name,
                result = DecisionResultResponse.from(decision),
                createdAt = decision.createdAt
            )
        }
    }
}

/**
 * Response for the computed decision result.
 *
 * All values are based on historical patterns.
 * This is descriptive, NOT prescriptive.
 */
data class DecisionResultResponse(
    /**
     * How well option A fits the user's historical patterns.
     * NOT a recommendation.
     */
    val probabilityA: Int,

    /**
     * How well option B fits the user's historical patterns.
     * NOT a recommendation.
     */
    val probabilityB: Int,

    /**
     * Estimated regret risk for option A based on historical data.
     * NOT a warning.
     */
    val regretRiskA: Int,

    /**
     * Estimated regret risk for option B based on historical data.
     * NOT a warning.
     */
    val regretRiskB: Int,

    /**
     * IDs of fragments used as evidence.
     */
    val evidenceFragmentIds: List<String>,

    /**
     * Value alignment scores for each axis.
     */
    val valueAlignment: Map<String, Double>
) {
    companion object {
        fun from(decision: Decision): DecisionResultResponse {
            val result = decision.result
            return DecisionResultResponse(
                probabilityA = result.probabilityA.percentage,
                probabilityB = result.probabilityB.percentage,
                regretRiskA = result.regretRiskA.percentage,
                regretRiskB = result.regretRiskB.percentage,
                evidenceFragmentIds = result.evidenceFragmentIds.map { it.toString() },
                valueAlignment = result.valueAlignment.mapKeys { it.key.name }
            )
        }
    }
}

/**
 * Response for a list of decisions.
 */
data class DecisionListResponse(
    val decisions: List<DecisionResponse>,
    val total: Long,
    val hasMore: Boolean
)

/**
 * Response for a decision explanation.
 *
 * The explanation describes WHY the calculation produced these results.
 * It does NOT recommend or advise.
 */
data class DecisionExplanationResponse(
    val decisionId: String,
    val summary: String,
    val evidenceSummary: String,
    val valueSummary: String
) {
    companion object {
        fun from(explanation: DecisionExplanation): DecisionExplanationResponse {
            return DecisionExplanationResponse(
                decisionId = explanation.decisionId.toString(),
                summary = explanation.summary,
                evidenceSummary = explanation.evidenceSummary,
                valueSummary = explanation.valueSummary
            )
        }
    }
}
