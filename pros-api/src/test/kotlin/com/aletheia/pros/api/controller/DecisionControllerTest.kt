package com.aletheia.pros.api.controller

import com.aletheia.pros.api.exception.GlobalExceptionHandler
import com.aletheia.pros.application.port.input.CreateDecisionCommand
import com.aletheia.pros.application.usecase.decision.CreateDecisionUseCase
import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.FeedbackId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.Decision
import com.aletheia.pros.domain.decision.DecisionFeedback
import com.aletheia.pros.domain.decision.DecisionRepository
import com.aletheia.pros.domain.decision.DecisionResult
import com.aletheia.pros.domain.decision.FeedbackType
import com.aletheia.pros.domain.decision.Probability
import com.aletheia.pros.domain.decision.RegretRisk
import com.aletheia.pros.domain.value.ValueAxis
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

@ExtendWith(MockKExtension::class)
@DisplayName("DecisionController Tests")
class DecisionControllerTest {

    @MockK
    private lateinit var createDecisionUseCase: CreateDecisionUseCase

    @MockK
    private lateinit var decisionRepository: DecisionRepository

    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper

    private val userId = UserId.generate()
    private val userIdHeader = userId.value.toString()

    @BeforeEach
    fun setUp() {
        val controller = DecisionController(
            createDecisionUseCase = createDecisionUseCase,
            decisionRepository = decisionRepository
        )
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
        objectMapper = ObjectMapper().findAndRegisterModules()
    }

    private fun createTestDecision(
        id: DecisionId = DecisionId.generate(),
        title: String = "Test Decision",
        optionA: String = "Option A",
        optionB: String = "Option B",
        priorityAxis: ValueAxis? = null
    ): Decision {
        val result = DecisionResult(
            probabilityA = Probability(0.6),
            probabilityB = Probability(0.4),
            regretRiskA = RegretRisk(0.2),
            regretRiskB = RegretRisk(0.3),
            evidenceFragmentIds = emptyList(),
            valueAlignment = mapOf(ValueAxis.GROWTH to 0.8, ValueAxis.STABILITY to 0.5)
        )
        return Decision(
            id = id,
            userId = userId,
            title = title,
            optionA = optionA,
            optionB = optionB,
            priorityAxis = priorityAxis,
            result = result,
            createdAt = Instant.now()
        )
    }

    @Nested
    @DisplayName("POST /v1/decisions - Create Decision")
    inner class CreateDecision {

        @Test
        fun `should create decision successfully`() {
            // Given
            val requestBody = mapOf(
                "title" to "Should I take the job offer?",
                "optionA" to "Accept the offer",
                "optionB" to "Stay at current job"
            )
            val createdDecision = createTestDecision(
                title = "Should I take the job offer?",
                optionA = "Accept the offer",
                optionB = "Stay at current job"
            )

            coEvery { createDecisionUseCase.execute(any()) } returns createdDecision

            // When/Then
            mockMvc.perform(
                post("/v1/decisions")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(createdDecision.id.toString()))
                .andExpect(jsonPath("$.title").value("Should I take the job offer?"))
                .andExpect(jsonPath("$.optionA").value("Accept the offer"))
                .andExpect(jsonPath("$.optionB").value("Stay at current job"))
                .andExpect(jsonPath("$.result.probabilityA").value(60))
                .andExpect(jsonPath("$.result.probabilityB").value(40))

            coVerify(exactly = 1) { createDecisionUseCase.execute(any()) }
        }

        @Test
        fun `should create decision with priority axis`() {
            // Given
            val requestBody = mapOf(
                "title" to "Career decision",
                "optionA" to "Option A",
                "optionB" to "Option B",
                "priorityAxis" to "GROWTH"
            )
            val createdDecision = createTestDecision(
                title = "Career decision",
                priorityAxis = ValueAxis.GROWTH
            )

            coEvery { createDecisionUseCase.execute(any()) } returns createdDecision

            // When/Then
            mockMvc.perform(
                post("/v1/decisions")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.priorityAxis").value("GROWTH"))
        }

        @Test
        fun `should handle invalid priority axis gracefully`() {
            // Given
            val requestBody = mapOf(
                "title" to "Career decision",
                "optionA" to "Option A",
                "optionB" to "Option B",
                "priorityAxis" to "INVALID_AXIS"
            )
            val createdDecision = createTestDecision(title = "Career decision")

            coEvery { createDecisionUseCase.execute(any()) } returns createdDecision

            // When/Then
            mockMvc.perform(
                post("/v1/decisions")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.priorityAxis").doesNotExist())
        }

        @Test
        fun `should return 400 when title is missing`() {
            // Given
            val requestBody = mapOf(
                "optionA" to "Option A",
                "optionB" to "Option B"
            )

            // When/Then
            mockMvc.perform(
                post("/v1/decisions")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `should return 400 when optionA is missing`() {
            // Given
            val requestBody = mapOf(
                "title" to "Test",
                "optionB" to "Option B"
            )

            // When/Then
            mockMvc.perform(
                post("/v1/decisions")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `should return 400 when optionB is missing`() {
            // Given
            val requestBody = mapOf(
                "title" to "Test",
                "optionA" to "Option A"
            )

            // When/Then
            mockMvc.perform(
                post("/v1/decisions")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `should return 400 when X-User-Id header is missing`() {
            // Given
            val requestBody = mapOf(
                "title" to "Test",
                "optionA" to "Option A",
                "optionB" to "Option B"
            )

            // When/Then
            mockMvc.perform(
                post("/v1/decisions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("Missing required header: X-User-Id"))
        }
    }

    @Nested
    @DisplayName("GET /v1/decisions/{id} - Get Decision by ID")
    inner class GetDecision {

        @Test
        fun `should return decision when found`() {
            // Given
            val decisionId = DecisionId.generate()
            val decision = createTestDecision(id = decisionId)

            every { decisionRepository.findById(decisionId) } returns decision

            // When/Then
            mockMvc.perform(
                get("/v1/decisions/${decisionId.value}")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(decisionId.toString()))
                .andExpect(jsonPath("$.title").value(decision.title))
        }

        @Test
        fun `should return 404 when decision not found`() {
            // Given
            val decisionId = DecisionId.generate()

            every { decisionRepository.findById(decisionId) } returns null

            // When/Then
            mockMvc.perform(
                get("/v1/decisions/${decisionId.value}")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should return 404 when decision belongs to different user`() {
            // Given
            val decisionId = DecisionId.generate()
            val differentUserId = UserId.generate()
            val decision = Decision(
                id = decisionId,
                userId = differentUserId,
                title = "Other user's decision",
                optionA = "Option A",
                optionB = "Option B",
                priorityAxis = null,
                result = DecisionResult(
                    probabilityA = Probability(0.5),
                    probabilityB = Probability(0.5),
                    regretRiskA = RegretRisk(0.2),
                    regretRiskB = RegretRisk(0.2),
                    evidenceFragmentIds = emptyList(),
                    valueAlignment = emptyMap()
                ),
                createdAt = Instant.now()
            )

            every { decisionRepository.findById(decisionId) } returns decision

            // When/Then
            mockMvc.perform(
                get("/v1/decisions/${decisionId.value}")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should return 400 when decision ID is invalid UUID`() {
            // When/Then
            mockMvc.perform(
                get("/v1/decisions/invalid-uuid")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("GET /v1/decisions - List Decisions")
    inner class ListDecisions {

        @Test
        fun `should return paginated decisions`() {
            // Given
            val decisions = listOf(
                createTestDecision(title = "Decision 1"),
                createTestDecision(title = "Decision 2")
            )

            every { decisionRepository.findByUserId(userId, 20, 0) } returns decisions
            every { decisionRepository.countByUserId(userId) } returns 10

            // When/Then
            mockMvc.perform(
                get("/v1/decisions")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.decisions.length()").value(2))
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.hasMore").value(true))
        }

        @Test
        fun `should use custom pagination parameters`() {
            // Given
            every { decisionRepository.findByUserId(userId, 10, 5) } returns emptyList()
            every { decisionRepository.countByUserId(userId) } returns 0

            // When/Then
            mockMvc.perform(
                get("/v1/decisions")
                    .header("X-User-Id", userIdHeader)
                    .param("limit", "10")
                    .param("offset", "5")
            )
                .andExpect(status().isOk)

            verify { decisionRepository.findByUserId(userId, 10, 5) }
        }

        @Test
        fun `should coerce limit to valid range`() {
            // Given
            every { decisionRepository.findByUserId(userId, 100, 0) } returns emptyList()
            every { decisionRepository.countByUserId(userId) } returns 0

            // When/Then - limit too high
            mockMvc.perform(
                get("/v1/decisions")
                    .header("X-User-Id", userIdHeader)
                    .param("limit", "500")
            )
                .andExpect(status().isOk)

            verify { decisionRepository.findByUserId(userId, 100, 0) }
        }

        @Test
        fun `should coerce negative offset to zero`() {
            // Given
            every { decisionRepository.findByUserId(userId, 20, 0) } returns emptyList()
            every { decisionRepository.countByUserId(userId) } returns 0

            // When/Then
            mockMvc.perform(
                get("/v1/decisions")
                    .header("X-User-Id", userIdHeader)
                    .param("offset", "-10")
            )
                .andExpect(status().isOk)

            verify { decisionRepository.findByUserId(userId, 20, 0) }
        }
    }

    @Nested
    @DisplayName("POST /v1/decisions/{id}/feedback - Submit Feedback")
    inner class SubmitFeedback {

        @Test
        fun `should submit feedback successfully`() {
            // Given
            val decisionId = DecisionId.generate()
            val decision = createTestDecision(id = decisionId)
            val feedback = DecisionFeedback(
                id = FeedbackId.generate(),
                decisionId = decisionId,
                feedbackType = FeedbackType.SATISFIED,
                createdAt = Instant.now()
            )

            every { decisionRepository.findById(decisionId) } returns decision
            every { decisionRepository.hasFeedback(decisionId) } returns false
            every { decisionRepository.saveFeedback(any()) } returns feedback

            val requestBody = mapOf("feedbackType" to "SATISFIED")

            // When/Then
            mockMvc.perform(
                post("/v1/decisions/${decisionId.value}/feedback")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.feedbackType").value("SATISFIED"))
                .andExpect(jsonPath("$.decisionId").value(decisionId.toString()))
        }

        @Test
        fun `should accept NEUTRAL feedback`() {
            // Given
            val decisionId = DecisionId.generate()
            val decision = createTestDecision(id = decisionId)
            val feedback = DecisionFeedback(
                id = FeedbackId.generate(),
                decisionId = decisionId,
                feedbackType = FeedbackType.NEUTRAL,
                createdAt = Instant.now()
            )

            every { decisionRepository.findById(decisionId) } returns decision
            every { decisionRepository.hasFeedback(decisionId) } returns false
            every { decisionRepository.saveFeedback(any()) } returns feedback

            val requestBody = mapOf("feedbackType" to "NEUTRAL")

            // When/Then
            mockMvc.perform(
                post("/v1/decisions/${decisionId.value}/feedback")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.feedbackType").value("NEUTRAL"))
        }

        @Test
        fun `should accept REGRET feedback`() {
            // Given
            val decisionId = DecisionId.generate()
            val decision = createTestDecision(id = decisionId)
            val feedback = DecisionFeedback(
                id = FeedbackId.generate(),
                decisionId = decisionId,
                feedbackType = FeedbackType.REGRET,
                createdAt = Instant.now()
            )

            every { decisionRepository.findById(decisionId) } returns decision
            every { decisionRepository.hasFeedback(decisionId) } returns false
            every { decisionRepository.saveFeedback(any()) } returns feedback

            val requestBody = mapOf("feedbackType" to "REGRET")

            // When/Then
            mockMvc.perform(
                post("/v1/decisions/${decisionId.value}/feedback")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.feedbackType").value("REGRET"))
        }

        @Test
        fun `should return 404 when decision not found`() {
            // Given
            val decisionId = DecisionId.generate()

            every { decisionRepository.findById(decisionId) } returns null

            val requestBody = mapOf("feedbackType" to "SATISFIED")

            // When/Then
            mockMvc.perform(
                post("/v1/decisions/${decisionId.value}/feedback")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should return 404 when decision belongs to different user`() {
            // Given
            val decisionId = DecisionId.generate()
            val differentUserId = UserId.generate()
            val decision = Decision(
                id = decisionId,
                userId = differentUserId,
                title = "Other user's decision",
                optionA = "Option A",
                optionB = "Option B",
                priorityAxis = null,
                result = DecisionResult(
                    probabilityA = Probability(0.5),
                    probabilityB = Probability(0.5),
                    regretRiskA = RegretRisk(0.2),
                    regretRiskB = RegretRisk(0.2),
                    evidenceFragmentIds = emptyList(),
                    valueAlignment = emptyMap()
                ),
                createdAt = Instant.now()
            )

            every { decisionRepository.findById(decisionId) } returns decision

            val requestBody = mapOf("feedbackType" to "SATISFIED")

            // When/Then
            mockMvc.perform(
                post("/v1/decisions/${decisionId.value}/feedback")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should return 409 when feedback already exists`() {
            // Given
            val decisionId = DecisionId.generate()
            val decision = createTestDecision(id = decisionId)

            every { decisionRepository.findById(decisionId) } returns decision
            every { decisionRepository.hasFeedback(decisionId) } returns true

            val requestBody = mapOf("feedbackType" to "SATISFIED")

            // When/Then
            mockMvc.perform(
                post("/v1/decisions/${decisionId.value}/feedback")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isConflict)
        }

        @Test
        fun `should return 400 when feedback type is invalid`() {
            // Given
            val decisionId = DecisionId.generate()

            val requestBody = mapOf("feedbackType" to "INVALID_TYPE")

            // When/Then
            mockMvc.perform(
                post("/v1/decisions/${decisionId.value}/feedback")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `should return 400 when feedback type is missing`() {
            // Given
            val decisionId = DecisionId.generate()

            val requestBody = emptyMap<String, Any>()

            // When/Then
            mockMvc.perform(
                post("/v1/decisions/${decisionId.value}/feedback")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("GET /v1/decisions/pending-feedback - Get Pending Feedback")
    inner class GetPendingFeedback {

        @Test
        fun `should return decisions needing feedback`() {
            // Given
            val decisions = listOf(
                createTestDecision(title = "Old decision 1"),
                createTestDecision(title = "Old decision 2")
            )

            every { decisionRepository.findDecisionsNeedingFeedback(userId) } returns decisions

            // When/Then
            mockMvc.perform(
                get("/v1/decisions/pending-feedback")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(2))
        }

        @Test
        fun `should return empty list when no pending feedback`() {
            // Given
            every { decisionRepository.findDecisionsNeedingFeedback(userId) } returns emptyList()

            // When/Then
            mockMvc.perform(
                get("/v1/decisions/pending-feedback")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(0))
        }

        @Test
        fun `should return 400 when X-User-Id header is missing`() {
            // When/Then
            mockMvc.perform(
                get("/v1/decisions/pending-feedback")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("Missing required header: X-User-Id"))
        }
    }
}
