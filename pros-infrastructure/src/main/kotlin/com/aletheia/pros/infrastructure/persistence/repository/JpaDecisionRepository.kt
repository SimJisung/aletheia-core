package com.aletheia.pros.infrastructure.persistence.repository

import com.aletheia.pros.infrastructure.persistence.entity.DecisionEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * Spring Data JPA Repository for DecisionEntity.
 */
@Repository
interface JpaDecisionRepository : JpaRepository<DecisionEntity, UUID> {

    /**
     * Finds all decisions for a user, ordered by creation time (newest first).
     */
    @Query("""
        SELECT d FROM DecisionEntity d
        WHERE d.userId = :userId
        ORDER BY d.createdAt DESC
    """)
    fun findByUserId(
        @Param("userId") userId: UUID,
        pageable: Pageable
    ): Page<DecisionEntity>

    /**
     * Finds decisions within a time range.
     */
    @Query("""
        SELECT d FROM DecisionEntity d
        WHERE d.userId = :userId
          AND d.createdAt BETWEEN :fromTime AND :toTime
        ORDER BY d.createdAt DESC
    """)
    fun findByUserIdAndTimeRange(
        @Param("userId") userId: UUID,
        @Param("fromTime") fromTime: Instant,
        @Param("toTime") toTime: Instant
    ): List<DecisionEntity>

    /**
     * Counts total decisions for a user.
     */
    fun countByUserId(userId: UUID): Long

    /**
     * Finds decisions that need feedback (24-72 hours old without feedback).
     */
    @Query("""
        SELECT d FROM DecisionEntity d
        LEFT JOIN d.feedback f
        WHERE d.userId = :userId
          AND f IS NULL
          AND d.createdAt BETWEEN :minTime AND :maxTime
        ORDER BY d.createdAt ASC
    """)
    fun findDecisionsNeedingFeedback(
        @Param("userId") userId: UUID,
        @Param("minTime") minTime: Instant,
        @Param("maxTime") maxTime: Instant
    ): List<DecisionEntity>
}
