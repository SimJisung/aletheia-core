package com.aletheia.pros.application.usecase.fragment

import com.aletheia.pros.application.port.input.FragmentListResult
import com.aletheia.pros.application.port.input.ListFragmentsQuery
import com.aletheia.pros.application.port.input.SimilarFragmentResult
import com.aletheia.pros.application.port.input.SimilarFragmentsQuery
import com.aletheia.pros.application.port.output.EmbeddingPort
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.fragment.FragmentRepository
import com.aletheia.pros.domain.fragment.ThoughtFragment

/**
 * Use case for querying thought fragments.
 *
 * Supports:
 * - Get by ID
 * - List with pagination
 * - Semantic similarity search
 */
class QueryFragmentUseCase(
    private val fragmentRepository: FragmentRepository,
    private val embeddingPort: EmbeddingPort
) {

    /**
     * Gets a fragment by ID.
     *
     * @param fragmentId The fragment ID
     * @return The fragment or null if not found/deleted
     */
    suspend fun getById(fragmentId: FragmentId): ThoughtFragment? {
        return fragmentRepository.findById(fragmentId)
    }

    /**
     * Lists fragments with pagination.
     *
     * @param query The list query parameters
     * @return Paginated result
     */
    suspend fun list(query: ListFragmentsQuery): FragmentListResult {
        val fragments = fragmentRepository.findByUserId(
            userId = query.userId,
            limit = query.limit + 1,  // Fetch one extra to check hasMore
            offset = query.offset
        )

        val hasMore = fragments.size > query.limit
        val resultFragments = if (hasMore) fragments.dropLast(1) else fragments
        val total = fragmentRepository.countByUserId(query.userId)

        return FragmentListResult(
            fragments = resultFragments,
            total = total,
            hasMore = hasMore
        )
    }

    /**
     * Finds semantically similar fragments.
     *
     * @param query The similarity search query
     * @return List of similar fragments with scores
     */
    suspend fun findSimilar(query: SimilarFragmentsQuery): List<SimilarFragmentResult> {
        // Generate embedding for query text
        val queryEmbedding = embeddingPort.embed(query.queryText)

        // Search for similar fragments
        val results = fragmentRepository.findSimilar(
            userId = query.userId,
            queryEmbedding = queryEmbedding,
            topK = query.topK
        )

        return results.map { similar ->
            SimilarFragmentResult(
                fragment = similar.fragment,
                similarity = similar.similarity
            )
        }
    }
}
