package com.aletheia.pros.domain.fragment

import com.aletheia.pros.domain.common.Embedding
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import java.time.Instant

/**
 * Repository interface for ThoughtFragment persistence.
 *
 * Implementation notes:
 * - save() should only INSERT, never UPDATE (append-only)
 * - Updates are only allowed for: embedding (set once), deletedAt (soft-delete)
 * - findAll methods should exclude soft-deleted fragments by default
 */
interface FragmentRepository {

    /**
     * Saves a new fragment.
     * This should INSERT only, never UPDATE existing records.
     */
    fun save(fragment: ThoughtFragment): ThoughtFragment

    /**
     * Finds a fragment by ID.
     * Returns null if not found or soft-deleted.
     */
    fun findById(id: FragmentId): ThoughtFragment?

    /**
     * Finds a fragment by ID, including soft-deleted ones.
     */
    fun findByIdIncludingDeleted(id: FragmentId): ThoughtFragment?

    /**
     * Finds all fragments for a user, ordered by creation time (newest first).
     * Excludes soft-deleted fragments.
     */
    fun findByUserId(
        userId: UserId,
        limit: Int = 100,
        offset: Int = 0
    ): List<ThoughtFragment>

    /**
     * Finds fragments by user within a time range.
     */
    fun findByUserIdAndTimeRange(
        userId: UserId,
        from: Instant,
        to: Instant
    ): List<ThoughtFragment>

    /**
     * Counts total fragments for a user (excluding deleted).
     */
    fun countByUserId(userId: UserId): Long

    /**
     * Soft-deletes a fragment by setting deletedAt.
     * Returns true if successful, false if already deleted or not found.
     */
    fun softDelete(id: FragmentId, deletedAt: Instant = Instant.now()): Boolean

    /**
     * Updates the embedding for a fragment.
     * This is one of the few allowed updates (embedding can only be set once).
     */
    fun updateEmbedding(id: FragmentId, embedding: Embedding): Boolean

    /**
     * Finds similar fragments using vector similarity search.
     *
     * @param userId User whose fragments to search
     * @param queryEmbedding The embedding to compare against
     * @param topK Number of results to return
     * @param minSimilarity Minimum similarity threshold (0.0 to 1.0)
     * @return List of fragments with their similarity scores, ordered by similarity (highest first)
     */
    fun findSimilar(
        userId: UserId,
        queryEmbedding: Embedding,
        topK: Int = 10,
        minSimilarity: Double = 0.0
    ): List<SimilarFragment>

    /**
     * Checks if a fragment exists (and is not deleted).
     */
    fun exists(id: FragmentId): Boolean
}

/**
 * A fragment with its similarity score.
 */
data class SimilarFragment(
    val fragment: ThoughtFragment,
    val similarity: Double
)
