package com.aletheia.pros.application.usecase.decision

import com.aletheia.pros.application.port.input.ListDecisionsQuery
import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.Decision
import com.aletheia.pros.domain.decision.DecisionRepository
import com.aletheia.pros.domain.decision.DecisionResult
import com.aletheia.pros.domain.decision.Probability
import com.aletheia.pros.domain.decision.RegretRisk
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
@DisplayName("QueryDecisionUseCase Tests")
class QueryDecisionUseCaseTest {

    @MockK
    private lateinit var decisionRepository: DecisionRepository

    private lateinit var useCase: QueryDecisionUseCase

    private val userId = UserId.generate()
    private val otherUserId = UserId.generate()

    @BeforeEach
    fun setUp() {
        useCase = QueryDecisionUseCase(decisionRepository)
    }

    private fun createTestDecision(
        userId: UserId = this.userId,
        title: String = "테스트 결정"
    ): Decision {
        return Decision(
            id = DecisionId.generate(),
            userId = userId,
            title = title,
            optionA = "옵션 A",
            optionB = "옵션 B",
            priorityAxis = null,
            result = DecisionResult(
                probabilityA = Probability(0.6),
                probabilityB = Probability(0.4),
                regretRiskA = RegretRisk(0.2),
                regretRiskB = RegretRisk(0.3),
                evidenceFragmentIds = listOf(FragmentId.generate()),
                valueAlignment = emptyMap()
            ),
            createdAt = Instant.now()
        )
    }

    @Nested
    @DisplayName("getDecision - Single Decision Lookup")
    inner class GetDecision {

        @Test
        fun `should return decision when found and owned by user`() = runBlocking {
            // Given
            val decision = createTestDecision()
            coEvery { decisionRepository.findById(decision.id) } returns decision

            // When
            val result = useCase.getDecision(decision.id, userId)

            // Then
            assertThat(result).isNotNull
            assertThat(result?.id).isEqualTo(decision.id)
            assertThat(result?.title).isEqualTo("테스트 결정")
            coVerify(exactly = 1) { decisionRepository.findById(decision.id) }
        }

        @Test
        fun `should return null when decision not found`() = runBlocking {
            // Given
            val decisionId = DecisionId.generate()
            coEvery { decisionRepository.findById(decisionId) } returns null

            // When
            val result = useCase.getDecision(decisionId, userId)

            // Then
            assertThat(result).isNull()
        }

        @Test
        fun `should return null when decision owned by different user`() = runBlocking {
            // Given
            val decision = createTestDecision(userId = otherUserId)
            coEvery { decisionRepository.findById(decision.id) } returns decision

            // When
            val result = useCase.getDecision(decision.id, userId)

            // Then
            assertThat(result).isNull()
        }

        @Test
        fun `should enforce ownership check for security`() = runBlocking {
            // Given - decision owned by otherUser
            val decision = createTestDecision(userId = otherUserId)
            coEvery { decisionRepository.findById(decision.id) } returns decision

            // When - requesting user tries to access
            val result = useCase.getDecision(decision.id, userId)

            // Then - should not expose decision to non-owner
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("listDecisions - Pagination")
    inner class ListDecisions {

        @Test
        fun `should return paginated list of decisions`() = runBlocking {
            // Given
            val decisions = (1..5).map { createTestDecision(title = "결정 $it") }
            val query = ListDecisionsQuery(userId = userId, limit = 10, offset = 0)

            coEvery { decisionRepository.findByUserId(userId, 10, 0) } returns decisions
            coEvery { decisionRepository.countByUserId(userId) } returns 5

            // When
            val result = useCase.listDecisions(query)

            // Then
            assertThat(result.decisions).hasSize(5)
            assertThat(result.total).isEqualTo(5)
            assertThat(result.hasMore).isFalse()
        }

        @Test
        fun `should indicate hasMore when more results exist`() = runBlocking {
            // Given
            val decisions = (1..10).map { createTestDecision(title = "결정 $it") }
            val query = ListDecisionsQuery(userId = userId, limit = 10, offset = 0)

            coEvery { decisionRepository.findByUserId(userId, 10, 0) } returns decisions
            coEvery { decisionRepository.countByUserId(userId) } returns 25

            // When
            val result = useCase.listDecisions(query)

            // Then
            assertThat(result.decisions).hasSize(10)
            assertThat(result.total).isEqualTo(25)
            assertThat(result.hasMore).isTrue()
        }

        @Test
        fun `should handle empty result`() = runBlocking {
            // Given
            val query = ListDecisionsQuery(userId = userId, limit = 10, offset = 0)

            coEvery { decisionRepository.findByUserId(userId, 10, 0) } returns emptyList()
            coEvery { decisionRepository.countByUserId(userId) } returns 0

            // When
            val result = useCase.listDecisions(query)

            // Then
            assertThat(result.decisions).isEmpty()
            assertThat(result.total).isEqualTo(0)
            assertThat(result.hasMore).isFalse()
        }

        @Test
        fun `should respect offset for pagination`() = runBlocking {
            // Given
            val decisions = (11..15).map { createTestDecision(title = "결정 $it") }
            val query = ListDecisionsQuery(userId = userId, limit = 5, offset = 10)

            coEvery { decisionRepository.findByUserId(userId, 5, 10) } returns decisions
            coEvery { decisionRepository.countByUserId(userId) } returns 20

            // When
            val result = useCase.listDecisions(query)

            // Then
            assertThat(result.decisions).hasSize(5)
            assertThat(result.hasMore).isTrue() // 10 + 5 < 20
            coVerify { decisionRepository.findByUserId(userId, 5, 10) }
        }

        @Test
        fun `should calculate hasMore correctly at boundary`() = runBlocking {
            // Given - exactly at the last page
            val decisions = (16..20).map { createTestDecision(title = "결정 $it") }
            val query = ListDecisionsQuery(userId = userId, limit = 5, offset = 15)

            coEvery { decisionRepository.findByUserId(userId, 5, 15) } returns decisions
            coEvery { decisionRepository.countByUserId(userId) } returns 20

            // When
            val result = useCase.listDecisions(query)

            // Then
            assertThat(result.hasMore).isFalse() // 15 + 5 >= 20
        }
    }

    @Nested
    @DisplayName("getPendingFeedbackDecisions - Feedback Queue")
    inner class GetPendingFeedbackDecisions {

        @Test
        fun `should return decisions needing feedback`() = runBlocking {
            // Given
            val pendingDecisions = (1..3).map { createTestDecision(title = "피드백 필요 $it") }
            coEvery { decisionRepository.findDecisionsNeedingFeedback(userId) } returns pendingDecisions

            // When
            val result = useCase.getPendingFeedbackDecisions(userId)

            // Then
            assertThat(result).hasSize(3)
            coVerify(exactly = 1) { decisionRepository.findDecisionsNeedingFeedback(userId) }
        }

        @Test
        fun `should return empty list when no pending feedback`() = runBlocking {
            // Given
            coEvery { decisionRepository.findDecisionsNeedingFeedback(userId) } returns emptyList()

            // When
            val result = useCase.getPendingFeedbackDecisions(userId)

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        fun `should only return decisions for specified user`() = runBlocking {
            // Given
            val userDecisions = listOf(createTestDecision(userId = userId))
            coEvery { decisionRepository.findDecisionsNeedingFeedback(userId) } returns userDecisions

            // When
            val result = useCase.getPendingFeedbackDecisions(userId)

            // Then
            assertThat(result).allMatch { it.userId == userId }
        }
    }
}
