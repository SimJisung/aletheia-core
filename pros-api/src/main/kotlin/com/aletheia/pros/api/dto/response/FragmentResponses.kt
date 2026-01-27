package com.aletheia.pros.api.dto.response

import com.aletheia.pros.domain.fragment.ThoughtFragment
import java.time.Instant

/**
 * Response for a single thought fragment.
 */
data class FragmentResponse(
    val id: String,
    val text: String,
    val createdAt: Instant,
    val moodValence: Double,
    val arousal: Double,
    val topicHint: String?,
    val hasEmbedding: Boolean
) {
    companion object {
        fun from(fragment: ThoughtFragment): FragmentResponse {
            return FragmentResponse(
                id = fragment.id.toString(),
                text = fragment.textRaw,
                createdAt = fragment.createdAt,
                moodValence = fragment.moodValence.value,
                arousal = fragment.arousal.value,
                topicHint = fragment.topicHint,
                hasEmbedding = fragment.hasEmbedding
            )
        }
    }
}

/**
 * Response for a list of fragments.
 */
data class FragmentListResponse(
    val fragments: List<FragmentResponse>,
    val total: Long,
    val hasMore: Boolean
)

/**
 * Response for similar fragment search.
 */
data class SimilarFragmentResponse(
    val fragment: FragmentResponse,
    val similarity: Double
)
