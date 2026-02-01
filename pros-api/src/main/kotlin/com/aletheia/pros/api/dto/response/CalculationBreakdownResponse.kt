package com.aletheia.pros.api.dto.response

import com.aletheia.pros.domain.decision.breakdown.CalculationBreakdown
import com.aletheia.pros.domain.decision.breakdown.CalculationParameters
import com.aletheia.pros.domain.decision.breakdown.FitBreakdown
import com.aletheia.pros.domain.decision.breakdown.FragmentContribution
import com.aletheia.pros.domain.decision.breakdown.RegretBreakdown
import com.aletheia.pros.domain.decision.breakdown.ScoreBreakdown

/**
 * API response for calculation breakdown.
 * Included when ?detail=true parameter is specified.
 */
data class CalculationBreakdownResponse(
    val fit: FitBreakdownResponse,
    val regret: RegretBreakdownResponse,
    val parameters: CalculationParametersResponse,
    val scores: ScoreBreakdownResponse
) {
    companion object {
        fun from(breakdown: CalculationBreakdown): CalculationBreakdownResponse {
            return CalculationBreakdownResponse(
                fit = FitBreakdownResponse.from(breakdown.fit),
                regret = RegretBreakdownResponse.from(breakdown.regret),
                parameters = CalculationParametersResponse.from(breakdown.parameters),
                scores = ScoreBreakdownResponse.from(breakdown.scores)
            )
        }
    }
}

/**
 * Response for pattern fit calculation breakdown.
 */
data class FitBreakdownResponse(
    /** Option A's fit score (0.0~1.0) */
    val fitScoreA: Double,
    /** Option B's fit score (0.0~1.0) */
    val fitScoreB: Double,
    /** Total weight used in calculation */
    val totalWeight: Double,
    /** Priority axis boost coefficient */
    val priorityAxisBoost: Double,
    /** Whether option A has higher fit */
    val isOptionAMoreFit: Boolean,
    /** Fit score difference (positive = A is more fit) */
    val fitDifference: Double,
    /** List of fragment contributions */
    val fragmentContributions: List<FragmentContributionResponse>
) {
    companion object {
        fun from(fit: FitBreakdown): FitBreakdownResponse {
            return FitBreakdownResponse(
                fitScoreA = fit.fitScoreA,
                fitScoreB = fit.fitScoreB,
                totalWeight = fit.totalWeight,
                priorityAxisBoost = fit.priorityAxisBoost,
                isOptionAMoreFit = fit.isOptionAMoreFit,
                fitDifference = fit.fitDifference,
                fragmentContributions = fit.fragmentContributions.map {
                    FragmentContributionResponse.from(it)
                }
            )
        }
    }
}

/**
 * Response for individual fragment contribution.
 */
data class FragmentContributionResponse(
    /** Fragment ID */
    val fragmentId: String,
    /** Fragment text summary (max 80 characters) */
    val fragmentSummary: String,
    /** Semantic similarity to decision context (0.0~1.0) */
    val similarity: Double,
    /** Emotional valence weight (0.0~1.0) */
    val valenceWeight: Double,
    /** Priority axis weight */
    val priorityWeight: Double,
    /** Contribution to option A's fit score */
    val contributionToA: Double,
    /** Contribution to option B's fit score */
    val contributionToB: Double,
    /** Which option this fragment favors */
    val favoredOption: String
) {
    companion object {
        fun from(contrib: FragmentContribution): FragmentContributionResponse {
            return FragmentContributionResponse(
                fragmentId = contrib.fragmentId.toString(),
                fragmentSummary = contrib.fragmentSummary,
                similarity = contrib.similarity,
                valenceWeight = contrib.valenceWeight,
                priorityWeight = contrib.priorityWeight,
                contributionToA = contrib.contributionToA,
                contributionToB = contrib.contributionToB,
                favoredOption = contrib.favoredOption.name
            )
        }
    }
}

/**
 * Response for regret risk calculation breakdown.
 */
data class RegretBreakdownResponse(
    /** Historical regret rate from past feedback (0.0~1.0) */
    val historicalRegretRate: Double,
    /** Valence variance of similar fragments (uncertainty indicator) */
    val valenceVariance: Double,
    /** Option A's alignment with negative fragments */
    val optionNegativityA: Double,
    /** Option B's alignment with negative fragments */
    val optionNegativityB: Double,
    /** Base regret value before option-specific adjustment */
    val baseRegret: Double,
    /** Final regret risk for option A */
    val regretRiskA: Double,
    /** Final regret risk for option B */
    val regretRiskB: Double,
    /** Number of feedback samples used */
    val feedbackCount: Int,
    /** Data reliability level based on feedback count */
    val dataReliability: String,
    /** Whether option A is safer (lower regret risk) */
    val isOptionASafer: Boolean,
    /** Calculation formula */
    val formula: String
) {
    companion object {
        fun from(regret: RegretBreakdown): RegretBreakdownResponse {
            return RegretBreakdownResponse(
                historicalRegretRate = regret.historicalRegretRate,
                valenceVariance = regret.valenceVariance,
                optionNegativityA = regret.optionNegativityA,
                optionNegativityB = regret.optionNegativityB,
                baseRegret = regret.baseRegret,
                regretRiskA = regret.regretRiskA,
                regretRiskB = regret.regretRiskB,
                feedbackCount = regret.feedbackCount,
                dataReliability = regret.dataReliability.name,
                isOptionASafer = regret.isOptionASafer,
                formula = regret.formula
            )
        }
    }
}

/**
 * Response for calculation parameters.
 */
data class CalculationParametersResponse(
    /** Regret sensitivity weight */
    val lambda: Double,
    /** Default regret rate when no feedback exists */
    val regretPrior: Double,
    /** Priority axis boost coefficient */
    val priorityAxisBoost: Double,
    /** Volatility weight in regret calculation */
    val volatilityWeight: Double,
    /** Negativity weight in regret calculation */
    val negativityWeight: Double
) {
    companion object {
        fun from(params: CalculationParameters): CalculationParametersResponse {
            return CalculationParametersResponse(
                lambda = params.lambda,
                regretPrior = params.regretPrior,
                priorityAxisBoost = params.priorityAxisBoost,
                volatilityWeight = params.volatilityWeight,
                negativityWeight = params.negativityWeight
            )
        }
    }
}

/**
 * Response for final score breakdown.
 */
data class ScoreBreakdownResponse(
    /** Final score for option A */
    val scoreA: Double,
    /** Final score for option B */
    val scoreB: Double,
    /** Score difference (positive = A has higher score) */
    val scoreDifference: Double,
    /** Calculation formula */
    val formula: String
) {
    companion object {
        fun from(scores: ScoreBreakdown): ScoreBreakdownResponse {
            return ScoreBreakdownResponse(
                scoreA = scores.scoreA,
                scoreB = scores.scoreB,
                scoreDifference = scores.scoreDifference,
                formula = scores.formula
            )
        }
    }
}
