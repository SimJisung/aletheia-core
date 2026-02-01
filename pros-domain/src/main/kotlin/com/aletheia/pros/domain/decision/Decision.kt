package com.aletheia.pros.domain.decision

import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.breakdown.CalculationBreakdown
import com.aletheia.pros.domain.value.ValueAxis
import java.time.Instant

/**
 * Decision represents a binary choice that the user wants to project.
 *
 * The system projects "how well each option fits the user's past patterns"
 * and "what is the risk of regret for each option".
 *
 * IMPORTANT: This is NOT a recommendation. It's a reflection of the user's
 * own historical patterns and values.
 *
 * MVP Constraint: Only A/B binary decisions are supported.
 */
data class Decision(
    val id: DecisionId,
    val userId: UserId,
    val title: String,
    val optionA: String,
    val optionB: String,
    val priorityAxis: ValueAxis?,
    val result: DecisionResult,
    val createdAt: Instant,
    /** LLM-generated explanation, cached after first generation */
    val explanation: DecisionExplanation? = null
) {
    init {
        require(title.isNotBlank()) { "Decision title cannot be blank" }
        require(optionA.isNotBlank()) { "Option A cannot be blank" }
        require(optionB.isNotBlank()) { "Option B cannot be blank" }
        require(title.length <= MAX_TITLE_LENGTH) {
            "Title exceeds maximum length of $MAX_TITLE_LENGTH"
        }
        require(optionA.length <= MAX_OPTION_LENGTH) {
            "Option A exceeds maximum length of $MAX_OPTION_LENGTH"
        }
        require(optionB.length <= MAX_OPTION_LENGTH) {
            "Option B exceeds maximum length of $MAX_OPTION_LENGTH"
        }
    }

    /**
     * Returns a new Decision with the explanation set.
     * Immutable update pattern.
     */
    fun withExplanation(explanation: DecisionExplanation): Decision {
        return copy(explanation = explanation)
    }

    /**
     * Whether this decision has a cached explanation.
     */
    val hasExplanation: Boolean
        get() = explanation != null

    companion object {
        const val MAX_TITLE_LENGTH = 500
        const val MAX_OPTION_LENGTH = 2000

        /**
         * Creates a new decision with computed results.
         */
        fun create(
            userId: UserId,
            title: String,
            optionA: String,
            optionB: String,
            priorityAxis: ValueAxis?,
            result: DecisionResult,
            createdAt: Instant = Instant.now()
        ): Decision = Decision(
            id = DecisionId.generate(),
            userId = userId,
            title = title,
            optionA = optionA,
            optionB = optionB,
            priorityAxis = priorityAxis,
            result = result,
            createdAt = createdAt,
            explanation = null
        )
    }
}

/**
 * DecisionExplanation contains LLM-generated explanation for a decision.
 *
 * This is cached in the database after first generation to avoid
 * repeated LLM calls.
 *
 * The explanation describes WHY results were computed.
 * It does NOT recommend or advise.
 */
data class DecisionExplanation(
    /** High-level summary of why results were computed */
    val summary: String,
    /** Summary of evidence fragments used */
    val evidenceSummary: String,
    /** Summary of relevant value considerations */
    val valueSummary: String,
    /** When this explanation was generated */
    val generatedAt: Instant = Instant.now()
) {
    init {
        require(summary.isNotBlank()) { "Summary cannot be blank" }
    }

    companion object {
        /**
         * Creates an explanation from LLM result.
         */
        fun create(
            summary: String,
            evidenceSummary: String,
            valueSummary: String
        ): DecisionExplanation = DecisionExplanation(
            summary = summary,
            evidenceSummary = evidenceSummary,
            valueSummary = valueSummary,
            generatedAt = Instant.now()
        )
    }
}

/**
 * DecisionResult contains the computed projection results.
 *
 * All values are based on the user's historical fragments and values.
 * The system does NOT recommend - it only reflects patterns.
 *
 * Output format is strictly defined:
 * - Probabilities (not recommendations)
 * - Regret risks (not warnings)
 * - Evidence (not justification)
 */
data class DecisionResult(
    val probabilityA: Probability,
    val probabilityB: Probability,
    val regretRiskA: RegretRisk,
    val regretRiskB: RegretRisk,
    val evidenceFragmentIds: List<FragmentId>,
    val valueAlignment: Map<ValueAxis, Double>,
    /** Calculation breakdown for explainability (nullable for backward compatibility) */
    val breakdown: CalculationBreakdown? = null
) {
    init {
        require(evidenceFragmentIds.size <= MAX_EVIDENCE_COUNT) {
            "Evidence fragments exceed maximum of $MAX_EVIDENCE_COUNT"
        }
    }

    /**
     * The higher probability option (NOT a recommendation).
     */
    val higherProbabilityOption: Option
        get() = if (probabilityA.value >= probabilityB.value) Option.A else Option.B

    /**
     * The lower regret risk option (NOT a recommendation).
     */
    val lowerRegretRiskOption: Option
        get() = if (regretRiskA.value <= regretRiskB.value) Option.A else Option.B

    companion object {
        const val MAX_EVIDENCE_COUNT = 10

        /**
         * Computes decision result using softmax (backward compatible, no breakdown).
         *
         * @param fitA Value fit score for option A
         * @param fitB Value fit score for option B
         * @param regretA Regret risk for option A
         * @param regretB Regret risk for option B
         * @param lambda Regret sensitivity weight
         * @param evidenceIds IDs of evidence fragments
         * @param valueAlignment Map of value axes to alignment scores
         */
        fun compute(
            fitA: Double,
            fitB: Double,
            regretA: Double,
            regretB: Double,
            lambda: Double,
            evidenceIds: List<FragmentId>,
            valueAlignment: Map<ValueAxis, Double>
        ): DecisionResult = computeWithBreakdown(
            fitA = fitA,
            fitB = fitB,
            regretA = regretA,
            regretB = regretB,
            lambda = lambda,
            evidenceIds = evidenceIds,
            valueAlignment = valueAlignment,
            breakdown = null
        )

        /**
         * Computes decision result with calculation breakdown for explainability.
         *
         * @param fitA Value fit score for option A
         * @param fitB Value fit score for option B
         * @param regretA Regret risk for option A
         * @param regretB Regret risk for option B
         * @param lambda Regret sensitivity weight
         * @param evidenceIds IDs of evidence fragments
         * @param valueAlignment Map of value axes to alignment scores
         * @param breakdown Optional calculation breakdown for detailed explanation
         */
        fun computeWithBreakdown(
            fitA: Double,
            fitB: Double,
            regretA: Double,
            regretB: Double,
            lambda: Double,
            evidenceIds: List<FragmentId>,
            valueAlignment: Map<ValueAxis, Double>,
            breakdown: CalculationBreakdown?
        ): DecisionResult {
            // score = fit - λ * regret
            val scoreA = fitA - lambda * regretA
            val scoreB = fitB - lambda * regretB

            // Softmax for probability
            val expA = kotlin.math.exp(scoreA)
            val expB = kotlin.math.exp(scoreB)
            val sumExp = expA + expB

            val probA = expA / sumExp
            val probB = expB / sumExp

            return DecisionResult(
                probabilityA = Probability(probA),
                probabilityB = Probability(probB),
                regretRiskA = RegretRisk(regretA.coerceIn(0.0, 1.0)),
                regretRiskB = RegretRisk(regretB.coerceIn(0.0, 1.0)),
                evidenceFragmentIds = evidenceIds.take(MAX_EVIDENCE_COUNT),
                valueAlignment = valueAlignment,
                breakdown = breakdown
            )
        }
    }
}

/**
 * Option represents A or B in a binary decision.
 */
enum class Option {
    A, B
}

/**
 * Probability value object.
 * Represents "how well this option fits the user's patterns".
 * NOT a recommendation or prediction of success.
 */
@JvmInline
value class Probability(val value: Double) {
    init {
        require(value in 0.0..1.0) {
            "Probability must be between 0.0 and 1.0, got: $value"
        }
    }

    val percentage: Int get() = (value * 100).toInt()

    override fun toString(): String = "${percentage}%"
}

/**
 * RegretRisk value object.
 * Represents the estimated likelihood of regret based on historical patterns.
 * NOT a warning or discouragement.
 */
@JvmInline
value class RegretRisk(val value: Double) {
    init {
        require(value in 0.0..1.0) {
            "RegretRisk must be between 0.0 and 1.0, got: $value"
        }
    }

    val isHigh: Boolean get() = value >= 0.6
    val isMedium: Boolean get() = value in 0.3..0.6
    val isLow: Boolean get() = value < 0.3

    val percentage: Int get() = (value * 100).toInt()

    override fun toString(): String = "${percentage}%"
}
