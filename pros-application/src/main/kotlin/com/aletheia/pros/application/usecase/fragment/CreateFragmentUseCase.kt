package com.aletheia.pros.application.usecase.fragment

import com.aletheia.pros.application.port.input.CreateFragmentCommand
import com.aletheia.pros.application.port.output.EmotionAnalysisPort
import com.aletheia.pros.application.port.output.EmbeddingPort
import com.aletheia.pros.domain.fragment.FragmentRepository
import com.aletheia.pros.domain.fragment.ThoughtFragment

/**
 * Use case for creating a new thought fragment.
 *
 * This use case orchestrates:
 * 1. Emotion analysis of the input text
 * 2. Embedding generation for semantic search
 * 3. Fragment persistence (append-only)
 *
 * Design Principles:
 * - Fragment text is IMMUTABLE once created
 * - Emotion analysis is a probabilistic signal, not a definitive judgment
 * - All fragments are stored, even contradictory ones
 */
class CreateFragmentUseCase(
    private val fragmentRepository: FragmentRepository,
    private val emotionAnalysisPort: EmotionAnalysisPort,
    private val embeddingPort: EmbeddingPort
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
        return fragmentRepository.save(fragmentWithEmbedding)
    }
}
