package com.aletheia.pros.infrastructure.persistence.repository

import com.aletheia.pros.infrastructure.persistence.entity.ThoughtFragmentEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * Spring Data JPA Repository for ThoughtFragmentEntity.
 *
 * Provides CRUD operations and vector similarity search.
 */
@Repository
interface JpaThoughtFragmentRepository : JpaRepository<ThoughtFragmentEntity, UUID> {

    /**
     * Finds all non-deleted fragments for a user, ordered by creation time (newest first).
     */
    @Query("""
        SELECT f FROM ThoughtFragmentEntity f
        WHERE f.userId = :userId AND f.deletedAt IS NULL
        ORDER BY f.createdAt DESC
    """)
    fun findByUserIdNotDeleted(
        @Param("userId") userId: UUID,
        pageable: Pageable
    ): Page<ThoughtFragmentEntity>

    /**
     * Finds non-deleted fragments within a time range.
     */
    @Query("""
        SELECT f FROM ThoughtFragmentEntity f
        WHERE f.userId = :userId
          AND f.deletedAt IS NULL
          AND f.createdAt BETWEEN :fromTime AND :toTime
        ORDER BY f.createdAt DESC
    """)
    fun findByUserIdAndTimeRange(
        @Param("userId") userId: UUID,
        @Param("fromTime") fromTime: Instant,
        @Param("toTime") toTime: Instant
    ): List<ThoughtFragmentEntity>

    /**
     * Counts non-deleted fragments for a user.
     */
    @Query("""
        SELECT COUNT(f) FROM ThoughtFragmentEntity f
        WHERE f.userId = :userId AND f.deletedAt IS NULL
    """)
    fun countByUserIdNotDeleted(@Param("userId") userId: UUID): Long

    /**
     * Soft-deletes a fragment by setting deletedAt.
     */
    @Modifying
    @Query("""
        UPDATE ThoughtFragmentEntity f
        SET f.deletedAt = :deletedAt
        WHERE f.id = :id AND f.deletedAt IS NULL
    """)
    fun softDelete(
        @Param("id") id: UUID,
        @Param("deletedAt") deletedAt: Instant
    ): Int

    /**
     * Updates the embedding for a fragment.
     * Uses CAST() instead of :: operator to avoid conflict with Spring Data JPA named parameter syntax.
     */
    @Modifying
    @Query(value = """
        UPDATE thought_fragments
        SET embedding = CAST(:embedding AS vector)
        WHERE id = :id AND embedding IS NULL
    """, nativeQuery = true)
    fun updateEmbedding(
        @Param("id") id: UUID,
        @Param("embedding") embedding: String
    ): Int

    /**
     * Finds similar fragments using pgvector cosine distance.
     * Returns fragments ordered by similarity (most similar first).
     *
     * Note: Uses native query for pgvector operations.
     * Uses CAST() instead of :: operator to avoid conflict with Spring Data JPA named parameter syntax.
     */
    @Query(value = """
        SELECT f.*, 1 - (f.embedding <=> CAST(:queryEmbedding AS vector)) as similarity
        FROM thought_fragments f
        WHERE f.user_id = :userId
          AND f.deleted_at IS NULL
          AND f.embedding IS NOT NULL
          AND 1 - (f.embedding <=> CAST(:queryEmbedding AS vector)) >= :minSimilarity
        ORDER BY f.embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :topK
    """, nativeQuery = true)
    fun findSimilarFragments(
        @Param("userId") userId: UUID,
        @Param("queryEmbedding") queryEmbedding: String,
        @Param("topK") topK: Int,
        @Param("minSimilarity") minSimilarity: Double
    ): List<Array<Any>>

    /**
     * Checks if a non-deleted fragment exists.
     */
    @Query("""
        SELECT COUNT(f) > 0 FROM ThoughtFragmentEntity f
        WHERE f.id = :id AND f.deletedAt IS NULL
    """)
    fun existsByIdNotDeleted(@Param("id") id: UUID): Boolean

    /**
     * Finds a fragment by ID, including deleted ones.
     */
    @Query("""
        SELECT f FROM ThoughtFragmentEntity f WHERE f.id = :id
    """)
    fun findByIdIncludingDeleted(@Param("id") id: UUID): ThoughtFragmentEntity?

    /**
     * Finds a fragment by ID, excluding deleted ones.
     */
    @Query("""
        SELECT f FROM ThoughtFragmentEntity f
        WHERE f.id = :id AND f.deletedAt IS NULL
    """)
    fun findByIdNotDeleted(@Param("id") id: UUID): ThoughtFragmentEntity?

    /**
     * Finds multiple fragments by their IDs, excluding deleted ones.
     */
    @Query("""
        SELECT f FROM ThoughtFragmentEntity f
        WHERE f.id IN :ids AND f.deletedAt IS NULL
    """)
    fun findByIdsNotDeleted(@Param("ids") ids: List<UUID>): List<ThoughtFragmentEntity>
}
