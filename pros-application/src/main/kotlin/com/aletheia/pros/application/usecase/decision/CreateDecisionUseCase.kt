package com.aletheia.pros.application.usecase.decision

import com.aletheia.pros.application.port.input.CreateDecisionCommand
import com.aletheia.pros.application.port.output.EmbeddingPort
import com.aletheia.pros.domain.common.Embedding
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.Decision
import com.aletheia.pros.domain.decision.DecisionRepository
import com.aletheia.pros.domain.decision.DecisionResult
import com.aletheia.pros.domain.decision.breakdown.CalculationBreakdown
import com.aletheia.pros.domain.decision.breakdown.CalculationParameters
import com.aletheia.pros.domain.decision.breakdown.FitBreakdown
import com.aletheia.pros.domain.decision.breakdown.FragmentContribution
import com.aletheia.pros.domain.decision.breakdown.RegretBreakdown
import com.aletheia.pros.domain.fragment.FragmentRepository
import com.aletheia.pros.domain.fragment.SimilarFragment
import com.aletheia.pros.domain.value.ValueAxis
import com.aletheia.pros.domain.value.ValueGraphRepository
import com.aletheia.pros.domain.value.ValueImportance
import com.aletheia.pros.domain.value.ValueImportanceRepository
import kotlin.math.exp

/**
 * Use case for creating a decision projection.
 *
 * CRITICAL DESIGN PRINCIPLE:
 * This calculates probabilities and regret risks based on historical patterns.
 * It does NOT recommend or suggest. The output is descriptive, not prescriptive.
 *
 * Calculation Flow:
 * 1. Generate context embedding from decision + options
 * 2. Find similar historical fragments (evidence)
 * 3. Calculate value fit for each option
 * 4. Estimate regret risk based on historical patterns
 * 5. Apply softmax to get probabilities
 */
class CreateDecisionUseCase(
    private val decisionRepository: DecisionRepository,
    private val fragmentRepository: FragmentRepository,
    private val valueGraphRepository: ValueGraphRepository,
    private val valueImportanceRepository: ValueImportanceRepository,
    private val embeddingPort: EmbeddingPort,
    private val userSettingsProvider: UserSettingsProvider
) {

    companion object {
        private const val EVIDENCE_COUNT = 5
        private const val SIMILAR_FRAGMENTS_COUNT = 20
        private const val PRIORITY_AXIS_BOOST = 0.35
        private const val VOLATILITY_WEIGHT = 0.3
        private const val NEGATIVITY_WEIGHT = 0.3

        // Value alignment calculation constants
        private const val DEFAULT_IMPORTANCE = 0.5
        private const val IMPLICIT_WEIGHT_FACTOR = 0.5
        private const val CONFIDENCE_THRESHOLD = 10.0
        private const val MAX_AMPLIFIED_DIFF = 4.0
    }

    /**
     * Creates a decision projection.
     */
    suspend fun execute(command: CreateDecisionCommand): Decision {
        // 1. Generate context embedding
        val contextText = buildContextText(command)
        val contextEmbedding = embeddingPort.embed(contextText)

        // 2. Find similar fragments as evidence
        val similarFragments = fragmentRepository.findSimilar(
            userId = command.userId,
            queryEmbedding = contextEmbedding,
            topK = SIMILAR_FRAGMENTS_COUNT
        )

        // 3. Get value graph
        val valueGraph = valueGraphRepository.findValueGraph(command.userId)

        // 3.5. Get user's explicit value importance
        val valueImportance = valueImportanceRepository.findByUserId(command.userId)

        // 4. Get user settings (lambda)
        val userSettings = userSettingsProvider.getSettings(command.userId)

        // 5. Generate option embeddings
        val optionAEmbedding = embeddingPort.embed(command.optionA)
        val optionBEmbedding = embeddingPort.embed(command.optionB)

        // 6. Calculate value fit with breakdown
        val fitBreakdown = calculateValueFitWithBreakdown(
            optionAEmbedding = optionAEmbedding,
            optionBEmbedding = optionBEmbedding,
            similarFragments = similarFragments,
            priorityAxis = command.priorityAxis
        )

        // 7. Calculate regret risk with breakdown
        val regretBreakdown = calculateRegretRiskWithBreakdown(
            userId = command.userId,
            similarFragments = similarFragments,
            basePrior = userSettings.regretPrior,
            optionAEmbedding = optionAEmbedding,
            optionBEmbedding = optionBEmbedding
        )

        // 8. Build calculation parameters
        val parameters = CalculationParameters.withUserSettings(
            lambda = userSettings.lambda,
            regretPrior = userSettings.regretPrior
        )

        // 9. Create calculation breakdown
        val calculationBreakdown = CalculationBreakdown.compute(
            fit = fitBreakdown,
            regret = regretBreakdown,
            parameters = parameters
        )

        // 10. Compute decision result with breakdown
        val evidenceIds = similarFragments
            .take(EVIDENCE_COUNT)
            .map { it.fragment.id }

        val valueAlignment = calculateValueAlignment(
            optionAEmbedding = optionAEmbedding,
            optionBEmbedding = optionBEmbedding,
            nodes = valueGraph?.nodes ?: emptyList(),
            importance = valueImportance
        )

        val result = DecisionResult.computeWithBreakdown(
            fitA = fitBreakdown.fitScoreA,
            fitB = fitBreakdown.fitScoreB,
            regretA = regretBreakdown.regretRiskA,
            regretB = regretBreakdown.regretRiskB,
            lambda = userSettings.lambda,
            evidenceIds = evidenceIds,
            valueAlignment = valueAlignment,
            breakdown = calculationBreakdown
        )

        // 11. Create and persist decision
        val decision = Decision.create(
            userId = command.userId,
            title = command.title,
            optionA = command.optionA,
            optionB = command.optionB,
            priorityAxis = command.priorityAxis,
            result = result
        )

        return decisionRepository.save(decision)
    }

    private fun buildContextText(command: CreateDecisionCommand): String {
        return buildString {
            append("Decision: ${command.title}\n")
            append("Option A: ${command.optionA}\n")
            append("Option B: ${command.optionB}")
            command.priorityAxis?.let {
                append("\nPriority: ${it.displayNameEn}")
            }
        }
    }

    /**
     * Calculates value fit with detailed breakdown for explainability.
     */
    private suspend fun calculateValueFitWithBreakdown(
        optionAEmbedding: Embedding,
        optionBEmbedding: Embedding,
        similarFragments: List<SimilarFragment>,
        priorityAxis: ValueAxis?
    ): FitBreakdown {
        if (similarFragments.isEmpty()) {
            return FitBreakdown.empty(PRIORITY_AXIS_BOOST)
        }

        val priorityEmbedding = priorityAxis?.let { axis ->
            embeddingPort.embed(buildPriorityAxisText(axis))
        }

        // Calculate weighted fit based on similar fragments
        var fitA = 0.0
        var fitB = 0.0
        var totalWeight = 0.0
        val contributions = mutableListOf<FragmentContribution>()

        for (similar in similarFragments) {
            val fragmentEmbedding = similar.fragment.embedding ?: continue

            val priorityWeight = priorityEmbedding?.let { axisEmbedding ->
                val axisSimilarity = fragmentEmbedding.cosineSimilarity(axisEmbedding)
                val axisRelevance = axisSimilarity.coerceAtLeast(0.0)
                1.0 + (PRIORITY_AXIS_BOOST * axisRelevance)
            } ?: 1.0

            val weight = similar.similarity * priorityWeight

            // Calculate how well each option aligns with this fragment
            val alignA = optionAEmbedding.cosineSimilarity(fragmentEmbedding)
            val alignB = optionBEmbedding.cosineSimilarity(fragmentEmbedding)

            // Weight by fragment's emotional valence (positive fragments = stronger signal)
            val valenceWeight = (1 + similar.fragment.moodValence.value) / 2

            val contribA = alignA * weight * valenceWeight
            val contribB = alignB * weight * valenceWeight

            fitA += contribA
            fitB += contribB
            totalWeight += weight

            // Capture contribution for breakdown
            contributions.add(
                FragmentContribution(
                    fragmentId = similar.fragment.id,
                    fragmentSummary = similar.fragment.textRaw.take(FragmentContribution.MAX_SUMMARY_LENGTH),
                    similarity = similar.similarity,
                    valenceWeight = valenceWeight,
                    priorityWeight = priorityWeight,
                    contributionToA = contribA,
                    contributionToB = contribB
                )
            )
        }

        if (totalWeight > 0) {
            fitA /= totalWeight
            fitB /= totalWeight
        }

        return FitBreakdown(
            fitScoreA = fitA.coerceIn(0.0, 1.0),
            fitScoreB = fitB.coerceIn(0.0, 1.0),
            totalWeight = totalWeight,
            priorityAxisBoost = PRIORITY_AXIS_BOOST,
            fragmentContributions = contributions
                .sortedByDescending { it.totalContribution }
                .take(FitBreakdown.MAX_CONTRIBUTIONS)
        )
    }

    private fun buildPriorityAxisText(axis: ValueAxis): String {
        return "Value axis: ${axis.displayNameEn}. ${axis.description}"
    }

    /**
     * Calculates regret risk with detailed breakdown for explainability.
     */
    private fun calculateRegretRiskWithBreakdown(
        userId: UserId,
        similarFragments: List<SimilarFragment>,
        basePrior: Double,
        optionAEmbedding: Embedding,
        optionBEmbedding: Embedding
    ): RegretBreakdown {
        // Get historical regret rate from past decisions
        val feedbackStats = decisionRepository.getFeedbackStats(userId)

        // Base regret risk from historical data
        val historicalRegretRate = if (feedbackStats.totalWithFeedback > 0) {
            feedbackStats.regretRate
        } else {
            basePrior
        }

        // Adjust based on emotional volatility of similar fragments
        val valenceVariance = calculateValenceVariance(similarFragments)

        val optionNegativityA = calculateOptionNegativity(optionAEmbedding, similarFragments)
        val optionNegativityB = calculateOptionNegativity(optionBEmbedding, similarFragments)

        // Higher variance = higher regret risk (uncertainty)
        val baseRegret = historicalRegretRate + valenceVariance * VOLATILITY_WEIGHT
        val regretA = (baseRegret + (optionNegativityA - 0.5) * NEGATIVITY_WEIGHT).coerceIn(0.0, 1.0)
        val regretB = (baseRegret + (optionNegativityB - 0.5) * NEGATIVITY_WEIGHT).coerceIn(0.0, 1.0)

        return RegretBreakdown(
            historicalRegretRate = historicalRegretRate,
            valenceVariance = valenceVariance,
            optionNegativityA = optionNegativityA,
            optionNegativityB = optionNegativityB,
            baseRegret = baseRegret,
            regretRiskA = regretA,
            regretRiskB = regretB,
            feedbackCount = feedbackStats.totalWithFeedback
        )
    }

    private fun calculateOptionNegativity(
        optionEmbedding: Embedding,
        similarFragments: List<SimilarFragment>
    ): Double {
        if (similarFragments.isEmpty()) return 0.5

        var weightedNegativity = 0.0
        var totalWeight = 0.0

        for (similar in similarFragments) {
            val fragmentEmbedding = similar.fragment.embedding ?: continue
            val alignment = optionEmbedding.cosineSimilarity(fragmentEmbedding)
            val alignmentWeight = ((alignment + 1.0) / 2.0).coerceIn(0.0, 1.0)
            if (alignmentWeight == 0.0) continue

            val negativity = ((1.0 - similar.fragment.moodValence.value) / 2.0)
                .coerceIn(0.0, 1.0)
            val weight = similar.similarity * alignmentWeight

            weightedNegativity += negativity * weight
            totalWeight += weight
        }

        return if (totalWeight == 0.0) 0.5
        else (weightedNegativity / totalWeight).coerceIn(0.0, 1.0)
    }

    private fun calculateValenceVariance(fragments: List<SimilarFragment>): Double {
        if (fragments.isEmpty()) return 0.5

        val valences = fragments.map { it.fragment.moodValence.value }
        val mean = valences.average()
        val variance = valences.map { (it - mean) * (it - mean) }.average()

        return variance.coerceIn(0.0, 1.0)
    }

    /**
     * Calculates how each option aligns with the user's value axes.
     *
     * IMPROVED ALGORITHM:
     * 1. Generate embedding for each ValueAxis description
     * 2. Calculate cosine similarity between each option and each axis
     * 3. Apply user's explicit importance weights (NEW)
     * 4. Apply user's implicit value node weights from fragments (improved)
     * 5. Normalize with amplification for better differentiation
     *
     * Output interpretation:
     * - 0.5 = neutral (both options align equally)
     * - >0.5 = option A aligns more with this axis
     * - <0.5 = option B aligns more with this axis
     *
     * This is purely descriptive - it describes how options relate to values,
     * NOT which option is "better".
     */
    private suspend fun calculateValueAlignment(
        optionAEmbedding: Embedding,
        optionBEmbedding: Embedding,
        nodes: List<com.aletheia.pros.domain.value.ValueNode>,
        importance: ValueImportance?
    ): Map<ValueAxis, Double> {
        val nodeMap = nodes.associateBy { it.axis }
        val alignments = mutableMapOf<ValueAxis, Double>()

        for (axis in ValueAxis.all()) {
            // Step 1: Generate embedding for this value axis
            val axisText = buildAxisText(axis)
            val axisEmbedding = embeddingPort.embed(axisText)

            // Step 2: Calculate similarity of each option to this axis
            val simA = optionAEmbedding.cosineSimilarity(axisEmbedding)
            val simB = optionBEmbedding.cosineSimilarity(axisEmbedding)

            // Step 3: Compute base difference
            val baseDiff = simA - simB  // Range: -1.0 to 1.0

            // Step 4: Apply explicit importance weight (NEW)
            // Higher importance = more amplification of the difference
            val explicitWeight = importance?.getImportance(axis) ?: DEFAULT_IMPORTANCE
            // Amplify difference for important values (1.0 + importance makes range 1.0-2.0)
            var amplifiedDiff = baseDiff * (1.0 + explicitWeight)

            // Step 5: Apply implicit weight from fragment history (IMPROVED)
            val node = nodeMap[axis]
            if (node != null && node.fragmentCount > 0) {
                // Confidence factor based on fragment count (caps at 1.0 when count >= 10)
                val confidence = minOf(node.fragmentCount / CONFIDENCE_THRESHOLD, 1.0)
                // Valence adjustment: positive valence amplifies, negative dampens
                val valenceAdjustment = 1.0 + (node.avgValence * IMPLICIT_WEIGHT_FACTOR * confidence)
                amplifiedDiff *= valenceAdjustment
            }

            // Step 6: Normalize to 0.0-1.0 range with better spread
            // Max possible diff after amplification is ~4.0 (baseDiff=1.0, explicitWeight=1.0, valence=1.0)
            val normalizedAlignment = (amplifiedDiff / MAX_AMPLIFIED_DIFF + 1.0) / 2.0

            alignments[axis] = normalizedAlignment.coerceIn(0.0, 1.0)
        }

        return alignments
    }

    /**
     * Builds descriptive text for a value axis to generate its embedding.
     */
    private fun buildAxisText(axis: ValueAxis): String {
        return "Value: ${axis.displayNameEn}. ${axis.description}"
    }
}

/**
 * Provider for user settings.
 */
interface UserSettingsProvider {
    suspend fun getSettings(userId: UserId): UserSettings

    /**
     * Updates lambda for a user based on feedback patterns.
     * @param userId The user to update
     * @param newLambda The new lambda value (will be clamped to valid range)
     */
    suspend fun updateLambda(userId: UserId, newLambda: Double)

    /**
     * Updates regret prior for a user based on historical data.
     * @param userId The user to update
     * @param newPrior The new prior value (will be clamped to 0.0-1.0)
     */
    suspend fun updateRegretPrior(userId: UserId, newPrior: Double)
}

/**
 * User settings for decision calculation.
 */
data class UserSettings(
    val lambda: Double = 1.0,
    val regretPrior: Double = 0.2
)
