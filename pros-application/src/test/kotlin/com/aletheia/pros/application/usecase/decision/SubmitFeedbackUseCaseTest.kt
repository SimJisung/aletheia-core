package com.aletheia.pros.application.usecase.decision

import com.aletheia.pros.application.port.input.SubmitFeedbackCommand
import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.Decision
import com.aletheia.pros.domain.decision.DecisionFeedback
import com.aletheia.pros.domain.decision.DecisionRepository
import com.aletheia.pros.domain.decision.DecisionResult
import com.aletheia.pros.domain.decision.FeedbackType
import com.aletheia.pros.domain.decision.Probability
import com.aletheia.pros.domain.decision.RegretRisk
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
@DisplayName("SubmitFeedbackUseCase Tests")
class SubmitFeedbackUseCaseTest {

    @MockK
    private lateinit var decisionRepository: DecisionRepository

    private lateinit var useCase: SubmitFeedbackUseCase

    private val userId = UserId.generate()
    private val otherUserId = UserId.generate()

    @BeforeEach
    fun setUp() {
        useCase = SubmitFeedbackUseCase(decisionRepository)
    }

    private fun createTestDecision(userId: UserId = this.userId): Decision {
        return Decision(
            id = DecisionId.generate(),
            userId = userId,
            title = "테스트 결정",
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
    @DisplayName("Successful Feedback Submission")
    inner class SuccessfulSubmission {

        @Test
        fun `should submit feedback successfully for CHOSE_A`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val command = SubmitFeedbackCommand(
                decisionId = decision.id,
                feedbackType = FeedbackType.CHOSE_A
            )

            coEvery { decisionRepository.findById(decision.id) } returns decision
            coEvery { decisionRepository.hasFeedback(decision.id) } returns false
            coEvery { decisionRepository.saveFeedback(any()) } answers { firstArg() }

            // When
            val result = useCase.execute(command, userId)

            // Then
            assertThat(result).isInstanceOf(FeedbackResult.Success::class.java)
            val success = result as FeedbackResult.Success
            assertThat(success.feedback.decisionId).isEqualTo(decision.id)
            assertThat(success.feedback.feedbackType).isEqualTo(FeedbackType.CHOSE_A)
        }

        @Test
        fun `should submit feedback successfully for CHOSE_B`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val command = SubmitFeedbackCommand(
                decisionId = decision.id,
                feedbackType = FeedbackType.CHOSE_B
            )

            coEvery { decisionRepository.findById(decision.id) } returns decision
            coEvery { decisionRepository.hasFeedback(decision.id) } returns false
            coEvery { decisionRepository.saveFeedback(any()) } answers { firstArg() }

            // When
            val result = useCase.execute(command, userId)

            // Then
            assertThat(result).isInstanceOf(FeedbackResult.Success::class.java)
            val success = result as FeedbackResult.Success
            assertThat(success.feedback.feedbackType).isEqualTo(FeedbackType.CHOSE_B)
        }

        @Test
        fun `should submit feedback successfully for POSTPONED`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val command = SubmitFeedbackCommand(
                decisionId = decision.id,
                feedbackType = FeedbackType.POSTPONED
            )

            coEvery { decisionRepository.findById(decision.id) } returns decision
            coEvery { decisionRepository.hasFeedback(decision.id) } returns false
            coEvery { decisionRepository.saveFeedback(any()) } answers { firstArg() }

            // When
            val result = useCase.execute(command, userId)

            // Then
            assertThat(result).isInstanceOf(FeedbackResult.Success::class.java)
            val success = result as FeedbackResult.Success
            assertThat(success.feedback.feedbackType).isEqualTo(FeedbackType.POSTPONED)
        }

        @Test
        fun `should save feedback with correct decision ID`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val command = SubmitFeedbackCommand(
                decisionId = decision.id,
                feedbackType = FeedbackType.CHOSE_A
            )

            val feedbackSlot = slot<DecisionFeedback>()
            coEvery { decisionRepository.findById(decision.id) } returns decision
            coEvery { decisionRepository.hasFeedback(decision.id) } returns false
            coEvery { decisionRepository.saveFeedback(capture(feedbackSlot)) } answers { firstArg() }

            // When
            useCase.execute(command, userId)

            // Then
            assertThat(feedbackSlot.captured.decisionId).isEqualTo(decision.id)
            coVerify(exactly = 1) { decisionRepository.saveFeedback(any()) }
        }
    }

    @Nested
    @DisplayName("Decision Not Found")
    inner class DecisionNotFound {

        @Test
        fun `should return NotFound when decision does not exist`() = runBlocking {
            // Given
            val decisionId = DecisionId.generate()
            val command = SubmitFeedbackCommand(
                decisionId = decisionId,
                feedbackType = FeedbackType.CHOSE_A
            )

            coEvery { decisionRepository.findById(decisionId) } returns null

            // When
            val result = useCase.execute(command, userId)

            // Then
            assertThat(result).isEqualTo(FeedbackResult.NotFound)
            coVerify(exactly = 0) { decisionRepository.saveFeedback(any()) }
        }
    }

    @Nested
    @DisplayName("Ownership Verification")
    inner class OwnershipVerification {

        @Test
        fun `should return NotFound when decision owned by different user`() = runBlocking {
            // Given
            val decision = createTestDecision(userId = otherUserId)
            val command = SubmitFeedbackCommand(
                decisionId = decision.id,
                feedbackType = FeedbackType.CHOSE_A
            )

            coEvery { decisionRepository.findById(decision.id) } returns decision

            // When
            val result = useCase.execute(command, userId)

            // Then
            assertThat(result).isEqualTo(FeedbackResult.NotFound)
            coVerify(exactly = 0) { decisionRepository.hasFeedback(any()) }
            coVerify(exactly = 0) { decisionRepository.saveFeedback(any()) }
        }

        @Test
        fun `should not expose ownership information in response`() = runBlocking {
            // Given - decision owned by another user
            val decision = createTestDecision(userId = otherUserId)
            val command = SubmitFeedbackCommand(
                decisionId = decision.id,
                feedbackType = FeedbackType.CHOSE_A
            )

            coEvery { decisionRepository.findById(decision.id) } returns decision

            // When
            val result = useCase.execute(command, userId)

            // Then - should return NotFound (not Forbidden) to avoid information leak
            assertThat(result).isEqualTo(FeedbackResult.NotFound)
        }
    }

    @Nested
    @DisplayName("Duplicate Feedback Prevention")
    inner class DuplicateFeedbackPrevention {

        @Test
        fun `should return AlreadyExists when feedback already submitted`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val command = SubmitFeedbackCommand(
                decisionId = decision.id,
                feedbackType = FeedbackType.CHOSE_A
            )

            coEvery { decisionRepository.findById(decision.id) } returns decision
            coEvery { decisionRepository.hasFeedback(decision.id) } returns true

            // When
            val result = useCase.execute(command, userId)

            // Then
            assertThat(result).isEqualTo(FeedbackResult.AlreadyExists)
            coVerify(exactly = 0) { decisionRepository.saveFeedback(any()) }
        }

        @Test
        fun `should check for existing feedback before saving`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val command = SubmitFeedbackCommand(
                decisionId = decision.id,
                feedbackType = FeedbackType.CHOSE_A
            )

            coEvery { decisionRepository.findById(decision.id) } returns decision
            coEvery { decisionRepository.hasFeedback(decision.id) } returns false
            coEvery { decisionRepository.saveFeedback(any()) } answers { firstArg() }

            // When
            useCase.execute(command, userId)

            // Then
            coVerify(exactly = 1) { decisionRepository.hasFeedback(decision.id) }
        }
    }

    @Nested
    @DisplayName("All Feedback Types")
    inner class AllFeedbackTypes {

        @Test
        fun `should handle REGRET_A feedback type`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val command = SubmitFeedbackCommand(
                decisionId = decision.id,
                feedbackType = FeedbackType.REGRET_A
            )

            coEvery { decisionRepository.findById(decision.id) } returns decision
            coEvery { decisionRepository.hasFeedback(decision.id) } returns false
            coEvery { decisionRepository.saveFeedback(any()) } answers { firstArg() }

            // When
            val result = useCase.execute(command, userId)

            // Then
            assertThat(result).isInstanceOf(FeedbackResult.Success::class.java)
            val success = result as FeedbackResult.Success
            assertThat(success.feedback.feedbackType).isEqualTo(FeedbackType.REGRET_A)
        }

        @Test
        fun `should handle REGRET_B feedback type`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val command = SubmitFeedbackCommand(
                decisionId = decision.id,
                feedbackType = FeedbackType.REGRET_B
            )

            coEvery { decisionRepository.findById(decision.id) } returns decision
            coEvery { decisionRepository.hasFeedback(decision.id) } returns false
            coEvery { decisionRepository.saveFeedback(any()) } answers { firstArg() }

            // When
            val result = useCase.execute(command, userId)

            // Then
            assertThat(result).isInstanceOf(FeedbackResult.Success::class.java)
            val success = result as FeedbackResult.Success
            assertThat(success.feedback.feedbackType).isEqualTo(FeedbackType.REGRET_B)
        }

        @Test
        fun `should handle SATISFIED feedback type`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val command = SubmitFeedbackCommand(
                decisionId = decision.id,
                feedbackType = FeedbackType.SATISFIED
            )

            coEvery { decisionRepository.findById(decision.id) } returns decision
            coEvery { decisionRepository.hasFeedback(decision.id) } returns false
            coEvery { decisionRepository.saveFeedback(any()) } answers { firstArg() }

            // When
            val result = useCase.execute(command, userId)

            // Then
            assertThat(result).isInstanceOf(FeedbackResult.Success::class.java)
            val success = result as FeedbackResult.Success
            assertThat(success.feedback.feedbackType).isEqualTo(FeedbackType.SATISFIED)
        }
    }

    @Nested
    @DisplayName("Execution Order Verification")
    inner class ExecutionOrderVerification {

        @Test
        fun `should verify execution order - find, check ownership, check existing, save`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val command = SubmitFeedbackCommand(
                decisionId = decision.id,
                feedbackType = FeedbackType.CHOSE_A
            )

            coEvery { decisionRepository.findById(decision.id) } returns decision
            coEvery { decisionRepository.hasFeedback(decision.id) } returns false
            coEvery { decisionRepository.saveFeedback(any()) } answers { firstArg() }

            // When
            useCase.execute(command, userId)

            // Then - verify order
            coVerify(ordering = io.mockk.Ordering.ORDERED) {
                decisionRepository.findById(decision.id)
                decisionRepository.hasFeedback(decision.id)
                decisionRepository.saveFeedback(any())
            }
        }

        @Test
        fun `should not check existing feedback if ownership check fails`() = runBlocking {
            // Given
            val decision = createTestDecision(userId = otherUserId)
            val command = SubmitFeedbackCommand(
                decisionId = decision.id,
                feedbackType = FeedbackType.CHOSE_A
            )

            coEvery { decisionRepository.findById(decision.id) } returns decision

            // When
            useCase.execute(command, userId)

            // Then
            coVerify(exactly = 1) { decisionRepository.findById(decision.id) }
            coVerify(exactly = 0) { decisionRepository.hasFeedback(any()) }
        }
    }
}
