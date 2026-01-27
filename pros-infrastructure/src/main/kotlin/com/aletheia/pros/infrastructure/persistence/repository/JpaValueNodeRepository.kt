package com.aletheia.pros.infrastructure.persistence.repository

import com.aletheia.pros.infrastructure.persistence.entity.ValueNodeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * Spring Data JPA Repository for ValueNodeEntity.
 */
@Repository
interface JpaValueNodeRepository : JpaRepository<ValueNodeEntity, UUID> {

    /**
     * Finds all value nodes for a user.
     */
    fun findByUserId(userId: UUID): List<ValueNodeEntity>

    /**
     * Finds a value node by user and axis.
     */
    @Query("""
        SELECT n FROM ValueNodeEntity n
        WHERE n.userId = :userId AND n.axis = :axis
    """)
    fun findByUserIdAndAxis(
        @Param("userId") userId: UUID,
        @Param("axis") axis: String
    ): ValueNodeEntity?

    /**
     * Checks if a user has value nodes initialized.
     */
    @Query("""
        SELECT COUNT(n) > 0 FROM ValueNodeEntity n
        WHERE n.userId = :userId
    """)
    fun existsByUserId(@Param("userId") userId: UUID): Boolean

    /**
     * Inserts a node if it doesn't already exist for (user_id, axis).
     */
    @Modifying
    @Query(
        value = """
            INSERT INTO value_nodes (
                id,
                user_id,
                axis,
                avg_valence,
                recent_trend,
                fragment_count,
                updated_at
            )
            VALUES (
                :id,
                :userId,
                :axis,
                :avgValence,
                :recentTrend,
                :fragmentCount,
                :updatedAt
            )
            ON CONFLICT (user_id, axis) DO NOTHING
        """,
        nativeQuery = true
    )
    fun insertIfAbsent(
        @Param("id") id: UUID,
        @Param("userId") userId: UUID,
        @Param("axis") axis: String,
        @Param("avgValence") avgValence: Double,
        @Param("recentTrend") recentTrend: String,
        @Param("fragmentCount") fragmentCount: Double,
        @Param("updatedAt") updatedAt: Instant
    ): Int

    /**
     * Counts nodes for a user.
     */
    fun countByUserId(userId: UUID): Long
}
