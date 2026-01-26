package com.aletheia.pros.application.port.input

import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.fragment.ThoughtFragment

/**
 * Input port for Fragment-related use cases.
 *
 * This is the primary port (driving adapter interface) that defines
 * what operations the outside world can perform on fragments.
 *
 * Implementing adapters: REST controllers, CLI, etc.
 */
interface FragmentInputPort {

    /**
     * Creates a new thought fragment.
     *
     * @param command The fragment creation command
     * @return The created fragment
     */
    suspend fun createFragment(command: CreateFragmentCommand): ThoughtFragment

    /**
     * Retrieves a fragment by ID.
     *
     * @param fragmentId The fragment ID
     * @return The fragment or null if not found
     */
    suspend fun getFragment(fragmentId: FragmentId): ThoughtFragment?

    /**
     * Lists fragments for a user.
     *
     * @param query The list query parameters
     * @return Paginated list of fragments
     */
    suspend fun listFragments(query: ListFragmentsQuery): FragmentListResult

    /**
     * Soft-deletes a fragment.
     *
     * @param fragmentId The fragment ID to delete
     * @return True if deleted, false if not found
     */
    suspend fun deleteFragment(fragmentId: FragmentId): Boolean

    /**
     * Finds similar fragments using semantic search.
     *
     * @param query The similarity search query
     * @return List of similar fragments with scores
     */
    suspend fun findSimilarFragments(query: SimilarFragmentsQuery): List<SimilarFragmentResult>
}

/**
 * Command to create a new thought fragment.
 */
data class CreateFragmentCommand(
    val userId: UserId,
    val text: String,
    val topicHint: String? = null
)

/**
 * Query parameters for listing fragments.
 */
data class ListFragmentsQuery(
    val userId: UserId,
    val limit: Int = 20,
    val offset: Int = 0
)

/**
 * Result of a fragment list query.
 */
data class FragmentListResult(
    val fragments: List<ThoughtFragment>,
    val total: Long,
    val hasMore: Boolean
)

/**
 * Query for semantic similarity search.
 */
data class SimilarFragmentsQuery(
    val userId: UserId,
    val queryText: String,
    val topK: Int = 10
)

/**
 * Result of similarity search.
 */
data class SimilarFragmentResult(
    val fragment: ThoughtFragment,
    val similarity: Double
)
