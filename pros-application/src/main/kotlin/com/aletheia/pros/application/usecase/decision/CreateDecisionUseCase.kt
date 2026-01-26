package com.aletheia.pros.application.usecase.decision

import com.aletheia.pros.application.port.input.CreateDecisionCommand
import com.aletheia.pros.application.port.output.EmbeddingPort
import com.aletheia.pros.domain.common.Embedding
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.Decision
import com.aletheia.pros.domain.decision.DecisionRepository
import com.aletheia.pros.domain.decision.DecisionResult
import com.aletheia.pros.domain.fragment.FragmentRepository
import com.aletheia.pros.domain.fragment.SimilarFragment
import com.aletheia.pros.domain.value.ValueAxis
import com.aletheia.pros.domain.value.ValueGraphRepository
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
    private val embeddingPort: EmbeddingPort,
    private val userSettingsProvider: UserSettingsProvider
) {

    companion object {
        private const val EVIDENCE_COUNT = 5
        private const val SIMILAR_FRAGMENTS_COUNT = 20
        private const val PRIORITY_AXIS_BOOST = 0.35
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

        // 4. Get user settings (lambda)
        val userSettings = userSettingsProvider.getSettings(command.userId)

        // 5. Generate option embeddings
        val optionAEmbedding = embeddingPort.embed(command.optionA)
        val optionBEmbedding = embeddingPort.embed(command.optionB)

        // 6. Calculate value fit for each option
        val (fitA, fitB) = calculateValueFit(
            optionAEmbedding = optionAEmbedding,
            optionBEmbedding = optionBEmbedding,
            similarFragments = similarFragments,
            priorityAxis = command.priorityAxis
        )

        // 7. Calculate regret risk
        val (regretA, regretB) = calculateRegretRisk(
            command.userId,
            similarFragments,
            userSettings.regretPrior,
            optionAEmbedding,
            optionBEmbedding
        )

        // 8. Compute decision result
        val evidenceIds = similarFragments
            .take(EVIDENCE_COUNT)
            .map { it.fragment.id }

        val valueAlignment = calculateValueAlignment(
            command.optionA,
            command.optionB,
            valueGraph?.nodes ?: emptyList()
        )

        val result = DecisionResult.compute(
            fitA = fitA,
            fitB = fitB,
            regretA = regretA,
            regretB = regretB,
            lambda = userSettings.lambda,
            evidenceIds = evidenceIds,
            valueAlignment = valueAlignment
        )

        // 9. Create and persist decision
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

    private suspend fun calculateValueFit(
        optionAEmbedding: Embedding,
        optionBEmbedding: Embedding,
        similarFragments: List<SimilarFragment>,
        priorityAxis: ValueAxis?
    ): Pair<Double, Double> {
        if (similarFragments.isEmpty()) {
            return 0.5 to 0.5
        }

        val priorityEmbedding = priorityAxis?.let { axis ->
            embeddingPort.embed(buildPriorityAxisText(axis))
        }

        // Calculate weighted fit based on similar fragments
        var fitA = 0.0
        var fitB = 0.0
        var totalWeight = 0.0

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

            fitA += alignA * weight * valenceWeight
            fitB += alignB * weight * valenceWeight
            totalWeight += weight
        }

        if (totalWeight > 0) {
            fitA /= totalWeight
            fitB /= totalWeight
        }

        return fitA to fitB
    }

    private fun buildPriorityAxisText(axis: ValueAxis): String {
        return "Value axis: ${axis.displayNameEn}. ${axis.description}"
    }

    private fun calculateRegretRisk(
        userId: UserId,
        similarFragments: List<SimilarFragment>,
        basePrior: Double,
        optionAEmbedding: Embedding,
        optionBEmbedding: Embedding
    ): Pair<Double, Double> {
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
        val baseRegret = historicalRegretRate + valenceVariance * 0.3
        val regretA = (baseRegret + (optionNegativityA - 0.5) * 0.3).coerceIn(0.0, 1.0)
        val regretB = (baseRegret + (optionNegativityB - 0.5) * 0.3).coerceIn(0.0, 1.0)

        return regretA to regretB
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

    private fun calculateValueAlignment(
        optionA: String,
        optionB: String,
        nodes: List<com.aletheia.pros.domain.value.ValueNode>
    ): Map<ValueAxis, Double> {
        // Placeholder: In production, use embeddings to calculate alignment
        // For now, return neutral alignment
        return ValueAxis.all().associateWith { 0.5 }
    }
}

/**
 * Provider for user settings.
 */
interface UserSettingsProvider {
    suspend fun getSettings(userId: UserId): UserSettings
}

/**
 * User settings for decision calculation.
 */
data class UserSettings(
    val lambda: Double = 1.0,
    val regretPrior: Double = 0.2
)
