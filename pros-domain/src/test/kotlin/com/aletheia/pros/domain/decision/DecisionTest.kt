package com.aletheia.pros.domain.decision

import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.value.ValueAxis
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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
            assertThat(p.percentage).isEqualTo(75.0)
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
        fun `should calculate complement correctly`() {
            val p = Probability(0.3)
            val complement = p.complement()

            assertThat(complement.value).isEqualTo(0.7, org.assertj.core.data.Offset.offset(0.001))
        }

        @Test
        fun `should identify high probability`() {
            val highP = Probability(0.8)
            val lowP = Probability(0.3)

            assertThat(highP.isHigh).isTrue()
            assertThat(lowP.isHigh).isFalse()
        }
    }

    @Nested
    @DisplayName("RegretRisk Value Object")
    inner class RegretRiskTests {

        @Test
        fun `should accept valid regret risk values`() {
            val risk = RegretRisk(0.25)
            assertThat(risk.value).isEqualTo(0.25)
            assertThat(risk.percentage).isEqualTo(25.0)
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
            val fitnessA = 3.0
            val fitnessB = 1.0
            val regretPrior = 0.2
            val lambda = 1.0
            val evidenceIds = listOf(FragmentId.generate(), FragmentId.generate())
            val valueAlignment = mapOf(
                ValueAxis.ACHIEVEMENT to 0.8,
                ValueAxis.SECURITY to -0.3
            )

            val result = DecisionResult.compute(
                fitnessA = fitnessA,
                fitnessB = fitnessB,
                regretPrior = regretPrior,
                lambda = lambda,
                evidenceFragmentIds = evidenceIds,
                valueAlignment = valueAlignment
            )

            // Softmax: P(A) = exp(3) / (exp(3) + exp(1)) ≈ 0.88
            assertThat(result.probabilityA.value).isGreaterThan(0.8)
            assertThat(result.probabilityB.value).isLessThan(0.2)

            // Probabilities should sum to 1
            assertThat(result.probabilityA.value + result.probabilityB.value)
                .isEqualTo(1.0, org.assertj.core.data.Offset.offset(0.001))

            assertThat(result.evidenceFragmentIds).hasSize(2)
            assertThat(result.valueAlignment).containsKeys(ValueAxis.ACHIEVEMENT, ValueAxis.SECURITY)
        }

        @Test
        fun `should handle equal fitness scores`() {
            val result = DecisionResult.compute(
                fitnessA = 1.0,
                fitnessB = 1.0,
                regretPrior = 0.2,
                lambda = 1.0,
                evidenceFragmentIds = emptyList(),
                valueAlignment = emptyMap()
            )

            // Equal fitness = 50/50
            assertThat(result.probabilityA.value).isEqualTo(0.5, org.assertj.core.data.Offset.offset(0.001))
            assertThat(result.probabilityB.value).isEqualTo(0.5, org.assertj.core.data.Offset.offset(0.001))
        }

        @Test
        fun `should calculate regret risk based on probability and prior`() {
            val result = DecisionResult.compute(
                fitnessA = 2.0,
                fitnessB = 0.5,
                regretPrior = 0.3,
                lambda = 1.5,
                evidenceFragmentIds = emptyList(),
                valueAlignment = emptyMap()
            )

            // Lower probability option should have higher regret risk
            assertThat(result.regretRiskB.value).isGreaterThan(result.regretRiskA.value)
        }

        @Test
        fun `should return stronger option correctly`() {
            val result = DecisionResult.compute(
                fitnessA = 3.0,
                fitnessB = 1.0,
                regretPrior = 0.2,
                lambda = 1.0,
                evidenceFragmentIds = emptyList(),
                valueAlignment = emptyMap()
            )

            assertThat(result.strongerOption).isEqualTo("A")
        }

        @Test
        fun `should calculate probability difference`() {
            val result = DecisionResult.compute(
                fitnessA = 2.0,
                fitnessB = 1.0,
                regretPrior = 0.2,
                lambda = 1.0,
                evidenceFragmentIds = emptyList(),
                valueAlignment = emptyMap()
            )

            val diff = result.probabilityDifference
            assertThat(diff).isGreaterThan(0.0)
            assertThat(diff).isLessThan(1.0)
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
                result = result
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
                result = result
            )

            assertThat(decision.priorityAxis).isNull()
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
            assertThat(feedback.isSatisfied).isTrue()
            assertThat(feedback.isRegret).isFalse()
        }

        @Test
        fun `should create regret feedback`() {
            val decisionId = DecisionId.generate()

            val feedback = DecisionFeedback.create(
                decisionId = decisionId,
                feedbackType = FeedbackType.REGRET
            )

            assertThat(feedback.feedbackType).isEqualTo(FeedbackType.REGRET)
            assertThat(feedback.isRegret).isTrue()
            assertThat(feedback.isSatisfied).isFalse()
        }

        @Test
        fun `should create neutral feedback`() {
            val decisionId = DecisionId.generate()

            val feedback = DecisionFeedback.create(
                decisionId = decisionId,
                feedbackType = FeedbackType.NEUTRAL
            )

            assertThat(feedback.feedbackType).isEqualTo(FeedbackType.NEUTRAL)
            assertThat(feedback.isSatisfied).isFalse()
            assertThat(feedback.isRegret).isFalse()
        }
    }

    // Helper methods
    private fun createDefaultResult(): DecisionResult {
        return DecisionResult.compute(
            fitnessA = 1.5,
            fitnessB = 1.0,
            regretPrior = 0.2,
            lambda = 1.0,
            evidenceFragmentIds = listOf(FragmentId.generate()),
            valueAlignment = mapOf(ValueAxis.ACHIEVEMENT to 0.5)
        )
    }
}
