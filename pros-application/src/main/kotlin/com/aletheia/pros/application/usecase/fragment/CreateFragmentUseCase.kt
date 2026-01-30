package com.aletheia.pros.application.usecase.fragment

import com.aletheia.pros.application.port.input.CreateFragmentCommand
import com.aletheia.pros.application.port.output.EmotionAnalysisPort
import com.aletheia.pros.application.port.output.EmbeddingPort
import com.aletheia.pros.application.port.output.ValueExtractionPort
import com.aletheia.pros.domain.fragment.FragmentRepository
import com.aletheia.pros.domain.fragment.ThoughtFragment
import com.aletheia.pros.domain.value.Trend
import com.aletheia.pros.domain.value.ValueGraphRepository
import com.aletheia.pros.domain.value.ValueNode
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Use case for creating a new thought fragment.
 *
 * This use case orchestrates:
 * 1. Emotion analysis of the input text
 * 2. Embedding generation for semantic search
 * 3. Value extraction and graph update
 * 4. Fragment persistence (append-only)
 *
 * Design Principles:
 * - Fragment text is IMMUTABLE once created
 * - Emotion analysis is a probabilistic signal, not a definitive judgment
 * - All fragments are stored, even contradictory ones
 * - Value graph updates are incremental and never destructive
 */
class CreateFragmentUseCase(
    private val fragmentRepository: FragmentRepository,
    private val emotionAnalysisPort: EmotionAnalysisPort,
    private val embeddingPort: EmbeddingPort,
    private val valueExtractionPort: ValueExtractionPort? = null,
    private val valueGraphRepository: ValueGraphRepository? = null
) {

    /**
     * Creates a new thought fragment.
     *
     * @param command The creation command with text and user info
     * @return The created fragment with computed emotion scores and embedding
     */
    suspend fun execute(command: CreateFragmentCommand): ThoughtFragment {
        // 1. Analyze emotion
        val emotionResult = emotionAnalysisPort.analyzeEmotion(command.text)

        // 2. Create fragment with emotion scores
        val fragment = ThoughtFragment.create(
            userId = command.userId,
            textRaw = command.text,
            moodValence = emotionResult.valence,
            arousal = emotionResult.arousal,
            topicHint = command.topicHint
        )

        // 3. Generate embedding
        val embedding = embeddingPort.embed(command.text)
        val fragmentWithEmbedding = fragment.withEmbedding(embedding)

        // 4. Persist (append-only)
        val savedFragment = fragmentRepository.save(fragmentWithEmbedding)

        // 5. Update value graph if ports are available
        updateValueGraph(command, emotionResult.valence.value)

        return savedFragment
    }

    /**
     * Updates the value graph based on extracted values from the fragment text.
     */
    private suspend fun updateValueGraph(
        command: CreateFragmentCommand,
        fragmentValence: Double
    ) {
        if (valueExtractionPort == null || valueGraphRepository == null) {
            logger.debug { "Value graph update skipped - ports not configured" }
            return
        }

        try {
            // Extract values from text
            val extractions = valueExtractionPort.extractValues(command.text)

            if (extractions.isEmpty()) {
                logger.debug { "No values extracted from fragment" }
                return
            }

            // Ensure value graph nodes exist
            valueGraphRepository.initializeNodesForUser(command.userId)

            // Update each detected value node
            extractions.filter { it.isSignificant }.forEach { extraction ->
                val existingNode = valueGraphRepository.findNodeByUserAndAxis(
                    command.userId,
                    extraction.axis
                ) ?: ValueNode.createDefault(command.userId, extraction.axis)

                // Combine fragment valence with extraction sentiment
                val combinedValence = (fragmentValence + extraction.sentiment) / 2.0

                val updatedNode = existingNode.updateWithFragment(
                    fragmentValence = combinedValence.coerceIn(-1.0, 1.0),
                    weight = extraction.confidence,
                    newTrend = Trend.NEUTRAL // Trend calculation requires history
                )

                valueGraphRepository.saveNode(updatedNode)
                logger.debug { "Updated value node ${extraction.axis} with weight ${extraction.confidence}" }
            }
        } catch (e: Exception) {
            // Value graph update failure should not fail fragment creation
            logger.warn(e) { "Failed to update value graph, continuing without update" }
        }
    }
}
