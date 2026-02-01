package com.aletheia.pros.domain.decision.breakdown

import com.aletheia.pros.domain.common.FragmentId

/**
 * Individual fragment's contribution to the decision calculation.
 *
 * Captures how much each historical fragment influenced the fit score
 * for each option.
 */
data class FragmentContribution(
    /** Fragment ID */
    val fragmentId: FragmentId,

    /** Fragment text summary (max 80 characters) */
    val fragmentSummary: String,

    /** Semantic similarity to decision context (0.0~1.0) */
    val similarity: Double,

    /** Emotional valence weight (0.0~1.0, higher = more positive) */
    val valenceWeight: Double,

    /** Priority axis weight (default 1.0, higher if related to priority axis) */
    val priorityWeight: Double,

    /** Contribution to option A's fit score */
    val contributionToA: Double,

    /** Contribution to option B's fit score */
    val contributionToB: Double
) {
    init {
        require(fragmentSummary.length <= MAX_SUMMARY_LENGTH) {
            "fragmentSummary exceeds $MAX_SUMMARY_LENGTH characters"
        }
        require(similarity in 0.0..1.0) {
            "similarity must be in [0.0, 1.0], got: $similarity"
        }
        require(valenceWeight in 0.0..1.0) {
            "valenceWeight must be in [0.0, 1.0], got: $valenceWeight"
        }
        require(priorityWeight >= 0.0) {
            "priorityWeight must be non-negative, got: $priorityWeight"
        }
    }

    /** Total contribution (A + B) */
    val totalContribution: Double
        get() = contributionToA + contributionToB

    /** Whether this fragment favors option A */
    val favorsOptionA: Boolean
        get() = contributionToA > contributionToB

    /** Which option this fragment favors more */
    val favoredOption: FavoredOption
        get() = when {
            contributionToA > contributionToB -> FavoredOption.A
            contributionToB > contributionToA -> FavoredOption.B
            else -> FavoredOption.NEUTRAL
        }

    companion object {
        const val MAX_SUMMARY_LENGTH = 80
    }
}

/**
 * Indicates which option a fragment favors.
 */
enum class FavoredOption {
    A, B, NEUTRAL
}
