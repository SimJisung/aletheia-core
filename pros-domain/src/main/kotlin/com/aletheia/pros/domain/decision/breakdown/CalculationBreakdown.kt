package com.aletheia.pros.domain.decision.breakdown

/**
 * Complete breakdown of the decision calculation.
 *
 * Aggregates all intermediate calculation values for explainability.
 * All values are deterministic and reproducible.
 *
 * DESIGN PRINCIPLE:
 * - Pure calculation values only (no LLM-generated text)
 * - Immutable object
 * - All values are reproducible given the same inputs
 */
data class CalculationBreakdown(
    /** Pattern fit calculation breakdown */
    val fit: FitBreakdown,

    /** Regret risk calculation breakdown */
    val regret: RegretBreakdown,

    /** Parameters used in calculation */
    val parameters: CalculationParameters,

    /** Final score calculation breakdown */
    val scores: ScoreBreakdown
) {
    companion object {
        /**
         * Computes a complete breakdown from fit and regret breakdowns.
         */
        fun compute(
            fit: FitBreakdown,
            regret: RegretBreakdown,
            parameters: CalculationParameters
        ): CalculationBreakdown {
            val scoreA = fit.fitScoreA - parameters.lambda * regret.regretRiskA
            val scoreB = fit.fitScoreB - parameters.lambda * regret.regretRiskB

            return CalculationBreakdown(
                fit = fit,
                regret = regret,
                parameters = parameters,
                scores = ScoreBreakdown(
                    scoreA = scoreA,
                    scoreB = scoreB,
                    formula = "score = fit - lambda * regret"
                )
            )
        }
    }
}

/**
 * Final score calculation breakdown.
 *
 * score = fit - lambda * regret
 * Probability is then computed using softmax on scores.
 */
data class ScoreBreakdown(
    /** Final score for option A */
    val scoreA: Double,

    /** Final score for option B */
    val scoreB: Double,

    /** Calculation formula */
    val formula: String
) {
    /** Score difference (positive = A has higher score) */
    val scoreDifference: Double
        get() = scoreA - scoreB

    /** Whether option A has higher score */
    val isOptionAHigher: Boolean
        get() = scoreA > scoreB
}

/**
 * Parameters used in the decision calculation.
 *
 * These are either user-specific settings or system defaults.
 */
data class CalculationParameters(
    /** Regret sensitivity weight (default 1.0) */
    val lambda: Double,

    /** Default regret rate when no feedback exists (default 0.2) */
    val regretPrior: Double,

    /** Priority axis boost coefficient (0.35) */
    val priorityAxisBoost: Double,

    /** Volatility weight in regret calculation (0.3) */
    val volatilityWeight: Double,

    /** Negativity weight in regret calculation (0.3) */
    val negativityWeight: Double
) {
    init {
        require(lambda >= 0.0) { "lambda must be non-negative, got: $lambda" }
        require(regretPrior in 0.0..1.0) { "regretPrior must be in [0.0, 1.0], got: $regretPrior" }
        require(priorityAxisBoost >= 0.0) { "priorityAxisBoost must be non-negative, got: $priorityAxisBoost" }
        require(volatilityWeight >= 0.0) { "volatilityWeight must be non-negative, got: $volatilityWeight" }
        require(negativityWeight >= 0.0) { "negativityWeight must be non-negative, got: $negativityWeight" }
    }

    companion object {
        /** Default system parameters */
        const val DEFAULT_LAMBDA = 1.0
        const val DEFAULT_REGRET_PRIOR = 0.2
        const val DEFAULT_PRIORITY_AXIS_BOOST = 0.35
        const val DEFAULT_VOLATILITY_WEIGHT = 0.3
        const val DEFAULT_NEGATIVITY_WEIGHT = 0.3

        /**
         * Creates parameters with user-specific lambda and regretPrior,
         * using system defaults for other values.
         */
        fun withUserSettings(lambda: Double, regretPrior: Double) = CalculationParameters(
            lambda = lambda,
            regretPrior = regretPrior,
            priorityAxisBoost = DEFAULT_PRIORITY_AXIS_BOOST,
            volatilityWeight = DEFAULT_VOLATILITY_WEIGHT,
            negativityWeight = DEFAULT_NEGATIVITY_WEIGHT
        )

        /**
         * Creates parameters with all default values.
         */
        fun defaults() = withUserSettings(DEFAULT_LAMBDA, DEFAULT_REGRET_PRIOR)
    }
}
