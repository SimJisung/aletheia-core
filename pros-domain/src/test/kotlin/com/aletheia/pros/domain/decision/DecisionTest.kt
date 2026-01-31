package com.aletheia.pros.domain.decision

import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.value.ValueAxis
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.data.Offset
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("Decision Domain Tests")
class DecisionTest {

    private val userId = UserId.generate()

    @Nested
    @DisplayName("Probability Value Object")
    inner class ProbabilityTests {

        @Test
        fun `should accept valid probability values`() {
            val p = Probability(0.75)
            assertThat(p.value).isEqualTo(0.75)
            assertThat(p.percentage).isEqualTo(75)
        }

        @Test
        fun `should accept boundary values 0 and 1`() {
            val p0 = Probability(0.0)
            val p1 = Probability(1.0)

            assertThat(p0.value).isEqualTo(0.0)
            assertThat(p1.value).isEqualTo(1.0)
        }

        @Test
        fun `should reject negative probability`() {
            assertThatThrownBy {
                Probability(-0.1)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `should reject probability greater than 1`() {
            assertThatThrownBy {
                Probability(1.1)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `should format toString correctly`() {
            val p = Probability(0.75)
            assertThat(p.toString()).isEqualTo("75%")
        }
    }

    @Nested
    @DisplayName("RegretRisk Value Object")
    inner class RegretRiskTests {

        @Test
        fun `should accept valid regret risk values`() {
            val risk = RegretRisk(0.25)
            assertThat(risk.value).isEqualTo(0.25)
            assertThat(risk.percentage).isEqualTo(25)
        }

        @Test
        fun `should identify high risk`() {
            val highRisk = RegretRisk(0.6)
            val lowRisk = RegretRisk(0.2)

            assertThat(highRisk.isHigh).isTrue()
            assertThat(lowRisk.isHigh).isFalse()
        }

        @Test
        fun `should identify low risk`() {
            val lowRisk = RegretRisk(0.15)
            assertThat(lowRisk.isLow).isTrue()
        }

        @Test
        fun `should identify medium risk`() {
            val mediumRisk = RegretRisk(0.4)
            assertThat(mediumRisk.isMedium).isTrue()
        }

        @Test
        fun `should reject invalid values`() {
            assertThatThrownBy { RegretRisk(-0.1) }
                .isInstanceOf(IllegalArgumentException::class.java)
            assertThatThrownBy { RegretRisk(1.1) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("DecisionResult")
    inner class DecisionResultTests {

        @Test
        fun `should compute result from fitness scores`() {
            val fitA = 3.0
            val fitB = 1.0
            val regretA = 0.2
            val regretB = 0.4
            val lambda = 1.0
            val evidenceIds = listOf(FragmentId.generate(), FragmentId.generate())
            val valueAlignment = mapOf(
                ValueAxis.ACHIEVEMENT to 0.8,
                ValueAxis.STABILITY to -0.3
            )

            val result = DecisionResult.compute(
                fitA = fitA,
                fitB = fitB,
                regretA = regretA,
                regretB = regretB,
                lambda = lambda,
                evidenceIds = evidenceIds,
                valueAlignment = valueAlignment
            )

            // Higher fit score should result in higher probability
            assertThat(result.probabilityA.value).isGreaterThan(result.probabilityB.value)

            // Probabilities should sum to 1
            assertThat(result.probabilityA.value + result.probabilityB.value)
                .isEqualTo(1.0, Offset.offset(0.001))

            assertThat(result.evidenceFragmentIds).hasSize(2)
            assertThat(result.valueAlignment).containsKeys(ValueAxis.ACHIEVEMENT, ValueAxis.STABILITY)
        }

        @Test
        fun `should handle equal fitness scores`() {
            val result = DecisionResult.compute(
                fitA = 1.0,
                fitB = 1.0,
                regretA = 0.2,
                regretB = 0.2,
                lambda = 1.0,
                evidenceIds = emptyList(),
                valueAlignment = emptyMap()
            )

            // Equal fitness = 50/50
            assertThat(result.probabilityA.value).isEqualTo(0.5, Offset.offset(0.001))
            assertThat(result.probabilityB.value).isEqualTo(0.5, Offset.offset(0.001))
        }

        @Test
        fun `should identify higher probability option`() {
            val result = DecisionResult.compute(
                fitA = 3.0,
                fitB = 1.0,
                regretA = 0.2,
                regretB = 0.2,
                lambda = 1.0,
                evidenceIds = emptyList(),
                valueAlignment = emptyMap()
            )

            assertThat(result.higherProbabilityOption).isEqualTo(Option.A)
        }

        @Test
        fun `should identify lower regret risk option`() {
            val result = DecisionResult.compute(
                fitA = 1.0,
                fitB = 1.0,
                regretA = 0.2,
                regretB = 0.5,
                lambda = 1.0,
                evidenceIds = emptyList(),
                valueAlignment = emptyMap()
            )

            assertThat(result.lowerRegretRiskOption).isEqualTo(Option.A)
        }
    }

    @Nested
    @DisplayName("Decision Entity")
    inner class DecisionEntityTests {

        @Test
        fun `should create decision with valid input`() {
            val result = createDefaultResult()

            val decision = Decision(
                id = DecisionId.generate(),
                userId = userId,
                title = "커리어 선택",
                optionA = "현재 회사 유지",
                optionB = "새 회사로 이직",
                priorityAxis = ValueAxis.ACHIEVEMENT,
                result = result,
                createdAt = Instant.now()
            )

            assertThat(decision.title).isEqualTo("커리어 선택")
            assertThat(decision.optionA).isEqualTo("현재 회사 유지")
            assertThat(decision.optionB).isEqualTo("새 회사로 이직")
            assertThat(decision.priorityAxis).isEqualTo(ValueAxis.ACHIEVEMENT)
        }

        @Test
        fun `should allow null priority axis`() {
            val result = createDefaultResult()

            val decision = Decision(
                id = DecisionId.generate(),
                userId = userId,
                title = "점심 메뉴",
                optionA = "한식",
                optionB = "양식",
                priorityAxis = null,
                result = result,
                createdAt = Instant.now()
            )

            assertThat(decision.priorityAxis).isNull()
        }

        @Test
        fun `should reject blank title`() {
            val result = createDefaultResult()

            assertThatThrownBy {
                Decision(
                    id = DecisionId.generate(),
                    userId = userId,
                    title = "",
                    optionA = "Option A",
                    optionB = "Option B",
                    priorityAxis = null,
                    result = result,
                    createdAt = Instant.now()
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("DecisionFeedback")
    inner class FeedbackTests {

        @Test
        fun `should create satisfied feedback`() {
            val decisionId = DecisionId.generate()

            val feedback = DecisionFeedback.create(
                decisionId = decisionId,
                feedbackType = FeedbackType.SATISFIED
            )

            assertThat(feedback.feedbackType).isEqualTo(FeedbackType.SATISFIED)
            assertThat(feedback.decisionId).isEqualTo(decisionId)
        }

        @Test
        fun `should create regret feedback`() {
            val decisionId = DecisionId.generate()

            val feedback = DecisionFeedback.create(
                decisionId = decisionId,
                feedbackType = FeedbackType.REGRET
            )

            assertThat(feedback.feedbackType).isEqualTo(FeedbackType.REGRET)
        }

        @Test
        fun `should create neutral feedback`() {
            val decisionId = DecisionId.generate()

            val feedback = DecisionFeedback.create(
                decisionId = decisionId,
                feedbackType = FeedbackType.NEUTRAL
            )

            assertThat(feedback.feedbackType).isEqualTo(FeedbackType.NEUTRAL)
        }

        @Test
        fun `should have correct regret signals`() {
            assertThat(FeedbackType.SATISFIED.regretSignal).isEqualTo(0.0)
            assertThat(FeedbackType.NEUTRAL.regretSignal).isEqualTo(0.3)
            assertThat(FeedbackType.REGRET.regretSignal).isEqualTo(1.0)
        }
    }

    // Helper methods
    private fun createDefaultResult(): DecisionResult {
        return DecisionResult.compute(
            fitA = 1.5,
            fitB = 1.0,
            regretA = 0.2,
            regretB = 0.3,
            lambda = 1.0,
            evidenceIds = listOf(FragmentId.generate()),
            valueAlignment = mapOf(ValueAxis.ACHIEVEMENT to 0.5)
        )
    }
}
