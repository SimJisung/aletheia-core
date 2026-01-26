package com.aletheia.pros.api.controller

import com.aletheia.pros.api.dto.request.CreateDecisionRequest
import com.aletheia.pros.api.dto.request.SubmitFeedbackRequest
import com.aletheia.pros.api.dto.response.DecisionListResponse
import com.aletheia.pros.api.dto.response.DecisionResponse
import com.aletheia.pros.api.dto.response.FeedbackResponse
import com.aletheia.pros.application.port.input.CreateDecisionCommand
import com.aletheia.pros.application.usecase.decision.CreateDecisionUseCase
import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.DecisionFeedback
import com.aletheia.pros.domain.decision.DecisionRepository
import com.aletheia.pros.domain.decision.FeedbackType
import com.aletheia.pros.domain.value.ValueAxis
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.PageRequest
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
    private val decisionRepository: DecisionRepository
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
        @PathVariable id: String
    ): ResponseEntity<DecisionResponse> {
        val decisionId = DecisionId(UUID.fromString(id))
        val decision = decisionRepository.findById(decisionId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(DecisionResponse.from(decision))
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
    ): ResponseEntity<DecisionListResponse> {
        val userIdObj = UserId(UUID.fromString(userId))
        val decisions = decisionRepository.findByUserId(
            userIdObj,
            limit.coerceIn(1, 100),
            offset.coerceAtLeast(0)
        )
        val total = decisionRepository.countByUserId(userIdObj)
        val hasMore = offset + decisions.size < total

        val response = DecisionListResponse(
            decisions = decisions.map { DecisionResponse.from(it) },
            total = total,
            hasMore = hasMore
        )

        return ResponseEntity.ok(response)
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
        @PathVariable id: String,
        @Valid @RequestBody request: SubmitFeedbackRequest
    ): ResponseEntity<FeedbackResponse> {
        val decisionId = DecisionId(UUID.fromString(id))

        // Check if decision exists
        decisionRepository.findById(decisionId)
            ?: return ResponseEntity.notFound().build()

        // Check if feedback already exists
        if (decisionRepository.hasFeedback(decisionId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build()
        }

        val feedbackType = FeedbackType.valueOf(request.feedbackType)
        val feedback = DecisionFeedback.create(
            decisionId = decisionId,
            feedbackType = feedbackType
        )

        val saved = decisionRepository.saveFeedback(feedback)
        return ResponseEntity.status(HttpStatus.CREATED).body(FeedbackResponse.from(saved))
    }

    /**
     * Gets decisions that need feedback (24-72 hours old without feedback).
     */
    @GetMapping("/pending-feedback")
    @Operation(summary = "Get decisions that need feedback")
    fun getDecisionsNeedingFeedback(
        @RequestHeader("X-User-Id") userId: String
    ): ResponseEntity<List<DecisionResponse>> {
        val userIdObj = UserId(UUID.fromString(userId))
        val decisions = decisionRepository.findDecisionsNeedingFeedback(userIdObj)

        return ResponseEntity.ok(decisions.map { DecisionResponse.from(it) })
    }
}
