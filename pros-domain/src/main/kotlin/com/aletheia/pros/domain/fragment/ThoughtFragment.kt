package com.aletheia.pros.domain.fragment

import com.aletheia.pros.domain.common.Embedding
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import java.time.Instant

/**
 * ThoughtFragment represents a single thought, feeling, or note captured by the user.
 *
 * Core Principles:
 * - IMMUTABLE: Once created, the content (textRaw) cannot be modified
 * - APPEND-ONLY: No updates allowed, only soft-delete
 * - TIMESTAMPED: Each fragment captures a specific moment in time
 *
 * The fragment includes:
 * - Raw text as written by the user
 * - Emotional state (valence + arousal)
 * - Optional topic hint for categorization
 * - Vector embedding for semantic search
 */
data class ThoughtFragment(
    val id: FragmentId,
    val userId: UserId,
    val textRaw: String,
    val createdAt: Instant,
    val moodValence: MoodValence,
    val arousal: Arousal,
    val topicHint: String? = null,
    val embedding: Embedding? = null,
    val deletedAt: Instant? = null
) {
    init {
        require(textRaw.isNotBlank()) { "Fragment text cannot be blank" }
        require(textRaw.length <= MAX_TEXT_LENGTH) {
            "Fragment text exceeds maximum length of $MAX_TEXT_LENGTH"
        }
    }

    /**
     * Whether this fragment has been soft-deleted.
     */
    val isDeleted: Boolean get() = deletedAt != null

    /**
     * Whether this fragment has an embedding computed.
     */
    val hasEmbedding: Boolean get() = embedding != null

    /**
     * Soft-deletes this fragment.
     * Returns a new instance with deletedAt set to now.
     *
     * Note: This is the ONLY mutation allowed on a fragment.
     */
    fun softDelete(deletedAt: Instant = Instant.now()): ThoughtFragment {
        require(!isDeleted) { "Fragment is already deleted" }
        return copy(deletedAt = deletedAt)
    }

    /**
     * Adds embedding to this fragment.
     * Returns a new instance with the embedding set.
     *
     * Note: Embedding can only be set once.
     */
    fun withEmbedding(embedding: Embedding): ThoughtFragment {
        require(!hasEmbedding) { "Fragment already has an embedding" }
        return copy(embedding = embedding)
    }

    companion object {
        const val MAX_TEXT_LENGTH = 10_000

        /**
         * Creates a new ThoughtFragment.
         */
        fun create(
            userId: UserId,
            textRaw: String,
            moodValence: MoodValence,
            arousal: Arousal,
            topicHint: String? = null,
            createdAt: Instant = Instant.now()
        ): ThoughtFragment = ThoughtFragment(
            id = FragmentId.generate(),
            userId = userId,
            textRaw = textRaw,
            createdAt = createdAt,
            moodValence = moodValence,
            arousal = arousal,
            topicHint = topicHint?.takeIf { it.isNotBlank() }
        )
    }
}

/**
 * Mood Valence represents the positive/negative dimension of emotion.
 *
 * Range: -1.0 (very negative) to +1.0 (very positive)
 *
 * Examples:
 * - +0.8: "I'm so excited about the promotion!"
 * - +0.3: "Had a nice lunch today"
 * - -0.2: "Traffic was annoying"
 * - -0.7: "Really frustrated with this situation"
 */
@JvmInline
value class MoodValence(val value: Double) {
    init {
        require(value in -1.0..1.0) {
            "MoodValence must be between -1.0 and 1.0, got: $value"
        }
    }

    val isPositive: Boolean get() = value > 0
    val isNegative: Boolean get() = value < 0
    val isNeutral: Boolean get() = value == 0.0

    companion object {
        val VERY_NEGATIVE = MoodValence(-1.0)
        val NEGATIVE = MoodValence(-0.5)
        val NEUTRAL = MoodValence(0.0)
        val POSITIVE = MoodValence(0.5)
        val VERY_POSITIVE = MoodValence(1.0)
    }
}

/**
 * Arousal represents the intensity/activation level of emotion.
 *
 * Range: 0.0 (calm/low energy) to 1.0 (excited/high energy)
 *
 * Examples:
 * - 0.9: Excitement, anger, fear (high activation)
 * - 0.5: Moderate engagement
 * - 0.2: Calm, relaxed, tired (low activation)
 */
@JvmInline
value class Arousal(val value: Double) {
    init {
        require(value in 0.0..1.0) {
            "Arousal must be between 0.0 and 1.0, got: $value"
        }
    }

    val isHigh: Boolean get() = value >= 0.7
    val isLow: Boolean get() = value <= 0.3
    val isModerate: Boolean get() = value in 0.3..0.7

    companion object {
        val VERY_LOW = Arousal(0.0)
        val LOW = Arousal(0.25)
        val MODERATE = Arousal(0.5)
        val HIGH = Arousal(0.75)
        val VERY_HIGH = Arousal(1.0)
    }
}
