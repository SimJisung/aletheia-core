package com.aletheia.pros.domain.decision.breakdown

/**
 * Breakdown of the pattern fit calculation.
 *
 * Captures how well each option aligns with the user's historical patterns
 * (fragments), including the contribution of each individual fragment.
 */
data class FitBreakdown(
    /** Option A's fit score (0.0~1.0) */
    val fitScoreA: Double,

    /** Option B's fit score (0.0~1.0) */
    val fitScoreB: Double,

    /** Total weight used in calculation */
    val totalWeight: Double,

    /** Priority axis boost coefficient (default 0.35) */
    val priorityAxisBoost: Double,

    /** List of fragment contributions (sorted by total contribution, max 10) */
    val fragmentContributions: List<FragmentContribution>
) {
    init {
        require(fitScoreA in 0.0..1.0) {
            "fitScoreA must be in [0.0, 1.0], got: $fitScoreA"
        }
        require(fitScoreB in 0.0..1.0) {
            "fitScoreB must be in [0.0, 1.0], got: $fitScoreB"
        }
        require(totalWeight >= 0.0) {
            "totalWeight must be non-negative, got: $totalWeight"
        }
        require(fragmentContributions.size <= MAX_CONTRIBUTIONS) {
            "fragmentContributions exceeds maximum of $MAX_CONTRIBUTIONS"
        }
    }

    /** Whether option A has higher fit */
    val isOptionAMoreFit: Boolean
        get() = fitScoreA > fitScoreB

    /** Fit score difference (positive = A is more fit) */
    val fitDifference: Double
        get() = fitScoreA - fitScoreB

    /** Absolute difference in fit scores */
    val fitGap: Double
        get() = kotlin.math.abs(fitDifference)

    /** Whether the fit scores are very close (< 0.05 difference) */
    val isCloseCall: Boolean
        get() = fitGap < CLOSE_CALL_THRESHOLD

    /** Number of fragments that favor option A */
    val fragmentsFavoringA: Int
        get() = fragmentContributions.count { it.favorsOptionA }

    /** Number of fragments that favor option B */
    val fragmentsFavoringB: Int
        get() = fragmentContributions.count { !it.favorsOptionA && it.contributionToB > it.contributionToA }

    companion object {
        const val MAX_CONTRIBUTIONS = 10
        const val CLOSE_CALL_THRESHOLD = 0.05

        /**
         * Creates an empty breakdown when no fragments are available.
         */
        fun empty(priorityAxisBoost: Double = 0.35): FitBreakdown = FitBreakdown(
            fitScoreA = 0.5,
            fitScoreB = 0.5,
            totalWeight = 0.0,
            priorityAxisBoost = priorityAxisBoost,
            fragmentContributions = emptyList()
        )
    }
}
