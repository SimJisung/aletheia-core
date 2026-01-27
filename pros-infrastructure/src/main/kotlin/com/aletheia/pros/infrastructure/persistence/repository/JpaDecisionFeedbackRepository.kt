package com.aletheia.pros.infrastructure.persistence.repository

import com.aletheia.pros.infrastructure.persistence.entity.DecisionFeedbackEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA Repository for DecisionFeedbackEntity.
 */
@Repository
interface JpaDecisionFeedbackRepository : JpaRepository<DecisionFeedbackEntity, UUID> {

    /**
     * Finds feedback by decision ID.
     */
    @Query("""
        SELECT f FROM DecisionFeedbackEntity f
        WHERE f.decision.id = :decisionId
    """)
    fun findByDecisionId(@Param("decisionId") decisionId: UUID): DecisionFeedbackEntity?

    /**
     * Checks if feedback exists for a decision.
     */
    @Query("""
        SELECT COUNT(f) > 0 FROM DecisionFeedbackEntity f
        WHERE f.decision.id = :decisionId
    """)
    fun existsByDecisionId(@Param("decisionId") decisionId: UUID): Boolean

    /**
     * Finds all feedback for a user's decisions.
     */
    @Query("""
        SELECT f FROM DecisionFeedbackEntity f
        WHERE f.decision.userId = :userId
        ORDER BY f.createdAt DESC
    """)
    fun findByUserId(@Param("userId") userId: UUID): List<DecisionFeedbackEntity>

    /**
     * Counts feedback by type for a user.
     */
    @Query("""
        SELECT f.feedbackType, COUNT(f)
        FROM DecisionFeedbackEntity f
        WHERE f.decision.userId = :userId
        GROUP BY f.feedbackType
    """)
    fun countByFeedbackTypeForUser(@Param("userId") userId: UUID): List<Array<Any>>

    /**
     * Counts total feedback for a user.
     */
    @Query("""
        SELECT COUNT(f) FROM DecisionFeedbackEntity f
        WHERE f.decision.userId = :userId
    """)
    fun countByUserId(@Param("userId") userId: UUID): Long
}
