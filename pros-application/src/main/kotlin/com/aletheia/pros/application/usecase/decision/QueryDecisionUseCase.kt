package com.aletheia.pros.application.usecase.decision

import com.aletheia.pros.application.port.input.DecisionListResult
import com.aletheia.pros.application.port.input.ListDecisionsQuery
import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.Decision
import com.aletheia.pros.domain.decision.DecisionRepository

/**
 * Use case for querying decisions.
 *
 * Provides single decision lookup, list with pagination,
 * and pending feedback queries.
 */
class QueryDecisionUseCase(
    private val decisionRepository: DecisionRepository
) {

    /**
     * Gets a decision by ID, with ownership verification.
     *
     * @param decisionId The decision ID to retrieve
     * @param userId The requesting user's ID (for ownership check)
     * @return The decision if found and owned by the user, null otherwise
     */
    suspend fun getDecision(decisionId: DecisionId, userId: UserId): Decision? {
        val decision = decisionRepository.findById(decisionId)
        // Ownership check - return null if not owned by user
        return if (decision?.userId == userId) decision else null
    }

    /**
     * Lists decisions for a user with pagination.
     *
     * @param query The list query with pagination parameters
     * @return The list result with decisions and pagination info
     */
    suspend fun listDecisions(query: ListDecisionsQuery): DecisionListResult {
        val decisions = decisionRepository.findByUserId(
            userId = query.userId,
            limit = query.limit,
            offset = query.offset
        )
        val total = decisionRepository.countByUserId(query.userId)
        val hasMore = query.offset + decisions.size < total

        return DecisionListResult(
            decisions = decisions,
            total = total,
            hasMore = hasMore
        )
    }

    /**
     * Gets decisions that need feedback (24-72 hours old without feedback).
     *
     * @param userId The user's ID
     * @return List of decisions awaiting feedback
     */
    suspend fun getPendingFeedbackDecisions(userId: UserId): List<Decision> {
        return decisionRepository.findDecisionsNeedingFeedback(userId)
    }
}
