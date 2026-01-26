package com.aletheia.pros.domain.decision

import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.FeedbackId
import com.aletheia.pros.domain.common.UserId
import java.time.Instant

/**
 * Repository interface for Decision and Feedback persistence.
 */
interface DecisionRepository {

    // ==================== Decision Operations ====================

    /**
     * Saves a new decision.
     */
    fun save(decision: Decision): Decision

    /**
     * Finds a decision by ID.
     */
    fun findById(id: DecisionId): Decision?

    /**
     * Finds all decisions for a user, ordered by creation time (newest first).
     */
    fun findByUserId(
        userId: UserId,
        limit: Int = 50,
        offset: Int = 0
    ): List<Decision>

    /**
     * Finds decisions within a time range.
     */
    fun findByUserIdAndTimeRange(
        userId: UserId,
        from: Instant,
        to: Instant
    ): List<Decision>

    /**
     * Counts total decisions for a user.
     */
    fun countByUserId(userId: UserId): Long

    // ==================== Feedback Operations ====================

    /**
     * Saves feedback for a decision.
     */
    fun saveFeedback(feedback: DecisionFeedback): DecisionFeedback

    /**
     * Finds feedback by ID.
     */
    fun findFeedbackById(id: FeedbackId): DecisionFeedback?

    /**
     * Finds feedback for a specific decision.
     */
    fun findFeedbackByDecisionId(decisionId: DecisionId): DecisionFeedback?

    /**
     * Finds all feedback for a user's decisions.
     */
    fun findFeedbacksByUserId(userId: UserId): List<DecisionFeedback>

    /**
     * Checks if feedback exists for a decision.
     */
    fun hasFeedback(decisionId: DecisionId): Boolean

    // ==================== Analytics Operations ====================

    /**
     * Finds decisions that need feedback (created 24-72 hours ago without feedback).
     */
    fun findDecisionsNeedingFeedback(userId: UserId): List<Decision>

    /**
     * Gets feedback statistics for a user.
     */
    fun getFeedbackStats(userId: UserId): FeedbackStats
}

/**
 * Statistics about user's feedback patterns.
 */
data class FeedbackStats(
    val totalDecisions: Int,
    val totalWithFeedback: Int,
    val satisfiedCount: Int,
    val neutralCount: Int,
    val regretCount: Int
) {
    /**
     * Overall regret rate (0.0 to 1.0).
     */
    val regretRate: Double
        get() = if (totalWithFeedback == 0) 0.0
        else regretCount.toDouble() / totalWithFeedback

    /**
     * Feedback completion rate (0.0 to 1.0).
     */
    val feedbackRate: Double
        get() = if (totalDecisions == 0) 0.0
        else totalWithFeedback.toDouble() / totalDecisions
}
