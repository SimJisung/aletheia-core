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
    val createdAt: Instant,
    /** Calculation breakdown for explainability (included when detail=true) */
    val breakdown: CalculationBreakdownResponse? = null,
    /** Cached LLM-generated explanation (null if not yet generated) */
    val explanation: DecisionExplanationResponse? = null
) {
    companion object {
        /**
         * Creates response without breakdown (default, backward compatible).
         */
        fun from(decision: Decision): DecisionResponse = from(decision, includeBreakdown = false)

        /**
         * Creates response with optional breakdown.
         *
         * @param decision The decision domain object
         * @param includeBreakdown Whether to include calculation breakdown
         */
        fun from(decision: Decision, includeBreakdown: Boolean): DecisionResponse {
            return DecisionResponse(
                id = decision.id.toString(),
                title = decision.title,
                optionA = decision.optionA,
                optionB = decision.optionB,
                priorityAxis = decision.priorityAxis?.name,
                result = DecisionResultResponse.from(decision),
                createdAt = decision.createdAt,
                breakdown = if (includeBreakdown) {
                    decision.result.breakdown?.let { CalculationBreakdownResponse.from(it) }
                } else null,
                explanation = decision.explanation?.let {
                    DecisionExplanationResponse(
                        decisionId = decision.id.toString(),
                        summary = it.summary,
                        evidenceSummary = it.evidenceSummary,
                        valueSummary = it.valueSummary
                    )
                }
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
