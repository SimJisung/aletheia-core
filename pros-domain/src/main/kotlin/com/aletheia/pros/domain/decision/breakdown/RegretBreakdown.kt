package com.aletheia.pros.domain.decision.breakdown

/**
 * Breakdown of the regret risk calculation.
 *
 * Regret risk is computed from:
 * 1. Historical regret rate from past feedback
 * 2. Emotional volatility (variance) of similar fragments
 * 3. Option negativity (alignment with negative fragments)
 *
 * Formula: regretRisk = baseRegret + (negativity - 0.5) * 0.3
 * Where: baseRegret = historicalRate + (variance * 0.3)
 */
data class RegretBreakdown(
    /** Historical regret rate from past feedback (0.0~1.0) */
    val historicalRegretRate: Double,

    /** Valence variance of similar fragments (0.0~1.0) - uncertainty indicator */
    val valenceVariance: Double,

    /** Option A's alignment with negative fragments (0.0~1.0) */
    val optionNegativityA: Double,

    /** Option B's alignment with negative fragments (0.0~1.0) */
    val optionNegativityB: Double,

    /** Base regret = historicalRate + (variance * 0.3) */
    val baseRegret: Double,

    /** Final regret risk for option A */
    val regretRiskA: Double,

    /** Final regret risk for option B */
    val regretRiskB: Double,

    /** Number of feedback samples used */
    val feedbackCount: Int,

    /** Calculation formula description */
    val formula: String = "baseRegret + (negativity - 0.5) * 0.3"
) {
    init {
        require(historicalRegretRate in 0.0..1.0) {
            "historicalRegretRate must be in [0.0, 1.0], got: $historicalRegretRate"
        }
        require(valenceVariance in 0.0..1.0) {
            "valenceVariance must be in [0.0, 1.0], got: $valenceVariance"
        }
        require(optionNegativityA in 0.0..1.0) {
            "optionNegativityA must be in [0.0, 1.0], got: $optionNegativityA"
        }
        require(optionNegativityB in 0.0..1.0) {
            "optionNegativityB must be in [0.0, 1.0], got: $optionNegativityB"
        }
        require(regretRiskA in 0.0..1.0) {
            "regretRiskA must be in [0.0, 1.0], got: $regretRiskA"
        }
        require(regretRiskB in 0.0..1.0) {
            "regretRiskB must be in [0.0, 1.0], got: $regretRiskB"
        }
        require(feedbackCount >= 0) {
            "feedbackCount must be non-negative, got: $feedbackCount"
        }
    }

    /** Data reliability based on feedback count */
    val dataReliability: DataReliability
        get() = when {
            feedbackCount >= HIGH_RELIABILITY_THRESHOLD -> DataReliability.HIGH
            feedbackCount >= MEDIUM_RELIABILITY_THRESHOLD -> DataReliability.MEDIUM
            else -> DataReliability.LOW
        }

    /** Whether option A is safer (lower regret risk) */
    val isOptionASafer: Boolean
        get() = regretRiskA < regretRiskB

    /** Regret risk difference (positive = B is riskier) */
    val regretRiskDifference: Double
        get() = regretRiskB - regretRiskA

    /** Whether using default prior (no feedback data) */
    val isUsingDefaultPrior: Boolean
        get() = feedbackCount == 0

    companion object {
        const val HIGH_RELIABILITY_THRESHOLD = 10
        const val MEDIUM_RELIABILITY_THRESHOLD = 3

        /**
         * Creates a default breakdown when no data is available.
         */
        fun withDefaultPrior(
            regretPrior: Double,
            optionNegativityA: Double = 0.5,
            optionNegativityB: Double = 0.5
        ): RegretBreakdown {
            val baseRegret = regretPrior
            val regretA = (baseRegret + (optionNegativityA - 0.5) * 0.3).coerceIn(0.0, 1.0)
            val regretB = (baseRegret + (optionNegativityB - 0.5) * 0.3).coerceIn(0.0, 1.0)

            return RegretBreakdown(
                historicalRegretRate = regretPrior,
                valenceVariance = 0.0,
                optionNegativityA = optionNegativityA,
                optionNegativityB = optionNegativityB,
                baseRegret = baseRegret,
                regretRiskA = regretA,
                regretRiskB = regretB,
                feedbackCount = 0
            )
        }
    }
}

/**
 * Data reliability level based on feedback sample size.
 */
enum class DataReliability {
    /** 10+ feedback samples - statistically meaningful */
    HIGH,

    /** 3-9 feedback samples - somewhat reliable */
    MEDIUM,

    /** 0-2 feedback samples - using default prior */
    LOW
}
