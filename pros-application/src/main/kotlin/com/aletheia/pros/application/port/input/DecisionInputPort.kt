package com.aletheia.pros.application.port.input

import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.Decision
import com.aletheia.pros.domain.decision.DecisionFeedback
import com.aletheia.pros.domain.decision.FeedbackType
import com.aletheia.pros.domain.value.ValueAxis

/**
 * Input port for Decision-related use cases.
 *
 * This is the primary port for decision projection operations.
 *
 * IMPORTANT DESIGN PRINCIPLE:
 * - The system projects probabilities based on historical patterns
 * - It does NOT recommend or suggest choices
 * - Output is always "probability + evidence", never "you should choose X"
 */
interface DecisionInputPort {

    /**
     * Creates a decision projection.
     *
     * This calculates:
     * - P(A|Me): Probability that A fits the user's patterns
     * - P(B|Me): Probability that B fits the user's patterns
     * - RegretRisk(A), RegretRisk(B): Estimated regret likelihood
     * - Evidence: Top fragments that informed the calculation
     *
     * @param command The decision creation command
     * @return The decision with computed results
     */
    suspend fun createDecision(command: CreateDecisionCommand): Decision

    /**
     * Retrieves a decision by ID.
     */
    suspend fun getDecision(decisionId: DecisionId): Decision?

    /**
     * Lists user's past decisions.
     */
    suspend fun listDecisions(query: ListDecisionsQuery): DecisionListResult

    /**
     * Submits feedback for a decision.
     *
     * Feedback is used to improve future projections by:
     * - Adjusting lambda (regret sensitivity)
     * - Updating value axis weights
     * - Refining regret priors
     */
    suspend fun submitFeedback(command: SubmitFeedbackCommand): DecisionFeedback

    /**
     * Gets decisions that need feedback (24-72 hours old without feedback).
     */
    suspend fun getPendingFeedbackDecisions(userId: UserId): List<Decision>

    /**
     * Gets the LLM-generated explanation for a decision.
     *
     * CONSTRAINTS:
     * - The explanation describes WHY these results were computed
     * - It does NOT recommend or advise
     * - It summarizes evidence without judgment
     */
    suspend fun getDecisionExplanation(decisionId: DecisionId): DecisionExplanation
}

/**
 * Command to create a decision projection.
 */
data class CreateDecisionCommand(
    val userId: UserId,
    val title: String,
    val optionA: String,
    val optionB: String,
    val priorityAxis: ValueAxis? = null
)

/**
 * Query for listing decisions.
 */
data class ListDecisionsQuery(
    val userId: UserId,
    val limit: Int = 20,
    val offset: Int = 0
)

/**
 * Result of decision list query.
 */
data class DecisionListResult(
    val decisions: List<Decision>,
    val total: Long,
    val hasMore: Boolean
)

/**
 * Command to submit feedback.
 */
data class SubmitFeedbackCommand(
    val decisionId: DecisionId,
    val feedbackType: FeedbackType
)

/**
 * LLM-generated explanation of a decision.
 *
 * This explains WHY the calculation produced these results.
 * It does NOT recommend or advise.
 */
data class DecisionExplanation(
    val decisionId: DecisionId,
    val summary: String,
    val evidenceSummary: String,
    val valueSummary: String
)
