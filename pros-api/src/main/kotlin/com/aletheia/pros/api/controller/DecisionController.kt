package com.aletheia.pros.api.controller

import com.aletheia.pros.api.dto.request.CreateDecisionRequest
import com.aletheia.pros.api.dto.request.SubmitFeedbackRequest
import com.aletheia.pros.api.dto.response.DecisionExplanationResponse
import com.aletheia.pros.api.dto.response.DecisionListResponse
import com.aletheia.pros.api.dto.response.DecisionResponse
import com.aletheia.pros.api.dto.response.FeedbackResponse
import com.aletheia.pros.application.port.input.CreateDecisionCommand
import com.aletheia.pros.application.port.input.ListDecisionsQuery
import com.aletheia.pros.application.port.input.SubmitFeedbackCommand
import com.aletheia.pros.application.usecase.decision.CreateDecisionUseCase
import com.aletheia.pros.application.usecase.decision.DecisionNotFoundException
import com.aletheia.pros.application.usecase.decision.FeedbackResult
import com.aletheia.pros.application.usecase.decision.GetDecisionExplanationUseCase
import com.aletheia.pros.application.usecase.decision.QueryDecisionUseCase
import com.aletheia.pros.application.usecase.decision.SubmitFeedbackUseCase
import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.FeedbackType
import com.aletheia.pros.domain.value.ValueAxis
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * REST Controller for Decision operations.
 *
 * IMPORTANT: Decisions are projections, NOT recommendations.
 * The probabilities reflect how well each option fits the user's
 * historical patterns. The user makes the final decision.
 */
@RestController
@RequestMapping("/v1/decisions")
@Tag(name = "Decisions", description = "Decision projection and feedback")
class DecisionController(
    private val createDecisionUseCase: CreateDecisionUseCase,
    private val getDecisionExplanationUseCase: GetDecisionExplanationUseCase,
    private val queryDecisionUseCase: QueryDecisionUseCase,
    private val submitFeedbackUseCase: SubmitFeedbackUseCase
) {

    /**
     * Creates a new decision projection.
     *
     * This analyzes the decision against the user's historical patterns
     * and returns probabilities for each option.
     *
     * NOTE: This is NOT a recommendation. The probabilities reflect
     * pattern fit, not "better" or "worse" options.
     */
    @PostMapping
    @Operation(summary = "Create a decision projection")
    fun createDecision(
        @RequestHeader("X-User-Id") userId: String,
        @Valid @RequestBody request: CreateDecisionRequest
    ): ResponseEntity<DecisionResponse> = runBlocking {
        val priorityAxis = request.priorityAxis?.let {
            try {
                ValueAxis.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        val command = CreateDecisionCommand(
            userId = UserId(UUID.fromString(userId)),
            title = request.title,
            optionA = request.optionA,
            optionB = request.optionB,
            priorityAxis = priorityAxis
        )

        val decision = createDecisionUseCase.execute(command)
        ResponseEntity.status(HttpStatus.CREATED).body(DecisionResponse.from(decision))
    }

    /**
     * Gets a decision by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a decision by ID")
    fun getDecision(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable id: String
    ): ResponseEntity<DecisionResponse> = runBlocking {
        val userIdObj = UserId(UUID.fromString(userId))
        val decisionId = DecisionId(UUID.fromString(id))

        val decision = queryDecisionUseCase.getDecision(decisionId, userIdObj)
            ?: return@runBlocking ResponseEntity.notFound().build<DecisionResponse>()

        ResponseEntity.ok(DecisionResponse.from(decision))
    }

    /**
     * Gets the LLM-generated explanation for a decision.
     *
     * The explanation describes WHY the calculation produced these results.
     * It does NOT recommend or advise.
     */
    @GetMapping("/{id}/explanation")
    @Operation(summary = "Get LLM-generated explanation for a decision")
    fun getDecisionExplanation(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable id: String
    ): ResponseEntity<DecisionExplanationResponse> = runBlocking {
        val userIdObj = UserId(UUID.fromString(userId))
        val decisionId = DecisionId(UUID.fromString(id))

        // Verify ownership first
        val decision = queryDecisionUseCase.getDecision(decisionId, userIdObj)
            ?: return@runBlocking ResponseEntity.notFound().build<DecisionExplanationResponse>()

        try {
            val explanation = getDecisionExplanationUseCase.execute(decisionId)
            ResponseEntity.ok(DecisionExplanationResponse.from(explanation))
        } catch (e: DecisionNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Lists decisions for a user with pagination.
     */
    @GetMapping
    @Operation(summary = "List decisions with pagination")
    fun listDecisions(
        @RequestHeader("X-User-Id") userId: String,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<DecisionListResponse> = runBlocking {
        val userIdObj = UserId(UUID.fromString(userId))

        val query = ListDecisionsQuery(
            userId = userIdObj,
            limit = limit.coerceIn(1, 100),
            offset = offset.coerceAtLeast(0)
        )

        val result = queryDecisionUseCase.listDecisions(query)

        val response = DecisionListResponse(
            decisions = result.decisions.map { DecisionResponse.from(it) },
            total = result.total,
            hasMore = result.hasMore
        )

        ResponseEntity.ok(response)
    }

    /**
     * Submits feedback for a decision.
     *
     * Feedback is used to improve future projections by adjusting:
     * - Regret sensitivity (lambda)
     * - Value axis weights
     */
    @PostMapping("/{id}/feedback")
    @Operation(summary = "Submit feedback for a decision")
    fun submitFeedback(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable id: String,
        @Valid @RequestBody request: SubmitFeedbackRequest
    ): ResponseEntity<FeedbackResponse> = runBlocking {
        val userIdObj = UserId(UUID.fromString(userId))
        val decisionId = DecisionId(UUID.fromString(id))

        val feedbackType = FeedbackType.valueOf(request.feedbackType)
        val command = SubmitFeedbackCommand(
            decisionId = decisionId,
            feedbackType = feedbackType
        )

        when (val result = submitFeedbackUseCase.execute(command, userIdObj)) {
            is FeedbackResult.Success ->
                ResponseEntity.status(HttpStatus.CREATED).body(FeedbackResponse.from(result.feedback))
            is FeedbackResult.NotFound ->
                ResponseEntity.notFound().build()
            is FeedbackResult.AlreadyExists ->
                ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    /**
     * Gets decisions that need feedback (24-72 hours old without feedback).
     */
    @GetMapping("/pending-feedback")
    @Operation(summary = "Get decisions that need feedback")
    fun getDecisionsNeedingFeedback(
        @RequestHeader("X-User-Id") userId: String
    ): ResponseEntity<List<DecisionResponse>> = runBlocking {
        val userIdObj = UserId(UUID.fromString(userId))
        val decisions = queryDecisionUseCase.getPendingFeedbackDecisions(userIdObj)

        ResponseEntity.ok(decisions.map { DecisionResponse.from(it) })
    }
}
