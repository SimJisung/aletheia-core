package com.aletheia.pros.application.usecase.decision

import com.aletheia.pros.application.port.output.ExplanationPort
import com.aletheia.pros.application.port.output.ExplanationResult
import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.Embedding
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.Decision
import com.aletheia.pros.domain.decision.DecisionRepository
import com.aletheia.pros.domain.decision.DecisionResult
import com.aletheia.pros.domain.decision.Probability
import com.aletheia.pros.domain.decision.RegretRisk
import com.aletheia.pros.domain.fragment.Arousal
import com.aletheia.pros.domain.fragment.FragmentRepository
import com.aletheia.pros.domain.fragment.MoodValence
import com.aletheia.pros.domain.fragment.ThoughtFragment
import com.aletheia.pros.domain.value.ValueAxis
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
@DisplayName("GetDecisionExplanationUseCase Tests")
class GetDecisionExplanationUseCaseTest {

    @MockK
    private lateinit var decisionRepository: DecisionRepository

    @MockK
    private lateinit var fragmentRepository: FragmentRepository

    @MockK
    private lateinit var explanationPort: ExplanationPort

    private lateinit var useCase: GetDecisionExplanationUseCase

    private val userId = UserId.generate()
    private val decisionId = DecisionId.generate()

    @BeforeEach
    fun setUp() {
        useCase = GetDecisionExplanationUseCase(
            decisionRepository = decisionRepository,
            fragmentRepository = fragmentRepository,
            explanationPort = explanationPort
        )
    }

    @Nested
    @DisplayName("Explanation Generation")
    inner class ExplanationGeneration {

        @Test
        fun `should generate explanation for valid decision`() = runBlocking {
            // Given
            val fragmentIds = listOf(FragmentId.generate(), FragmentId.generate())
            val decision = createMockDecision(fragmentIds)
            val fragments = fragmentIds.map { createMockFragment(it) }
            val expectedResult = ExplanationResult(
                summary = "이 결정은 과거 패턴을 기반으로 계산되었습니다.",
                evidenceSummary = "관련 기록 2개를 분석했습니다.",
                valueSummary = "안정성과 성장 가치가 관련됩니다."
            )

            coEvery { decisionRepository.findById(decisionId) } returns decision
            coEvery { fragmentRepository.findByIds(fragmentIds) } returns fragments
            coEvery { explanationPort.explainDecision(decision, fragments) } returns expectedResult

            // When
            val result = useCase.execute(decisionId)

            // Then
            assertThat(result.decisionId).isEqualTo(decisionId)
            assertThat(result.summary).isEqualTo(expectedResult.summary)
            assertThat(result.evidenceSummary).isEqualTo(expectedResult.evidenceSummary)
            assertThat(result.valueSummary).isEqualTo(expectedResult.valueSummary)

            coVerify { explanationPort.explainDecision(decision, fragments) }
        }

        @Test
        fun `should throw exception when decision not found`() {
            // Given
            coEvery { decisionRepository.findById(decisionId) } returns null

            // When & Then
            assertThatThrownBy {
                runBlocking { useCase.execute(decisionId) }
            }.isInstanceOf(DecisionNotFoundException::class.java)
        }

        @Test
        fun `should handle decision with no evidence fragments`() = runBlocking {
            // Given
            val decision = createMockDecision(emptyList())
            val expectedResult = ExplanationResult(
                summary = "증거 없이 계산되었습니다.",
                evidenceSummary = "관련 기록이 없습니다.",
                valueSummary = "기본값으로 계산되었습니다."
            )

            coEvery { decisionRepository.findById(decisionId) } returns decision
            coEvery { fragmentRepository.findByIds(emptyList()) } returns emptyList()
            coEvery { explanationPort.explainDecision(decision, emptyList()) } returns expectedResult

            // When
            val result = useCase.execute(decisionId)

            // Then
            assertThat(result.decisionId).isEqualTo(decisionId)
            assertThat(result.summary).isEqualTo(expectedResult.summary)
        }
    }

    private fun createMockDecision(evidenceIds: List<FragmentId>): Decision {
        val result = DecisionResult(
            probabilityA = Probability(0.6),
            probabilityB = Probability(0.4),
            regretRiskA = RegretRisk(0.2),
            regretRiskB = RegretRisk(0.3),
            evidenceFragmentIds = evidenceIds,
            valueAlignment = ValueAxis.all().associateWith { 0.5 }
        )

        return Decision(
            id = decisionId,
            userId = userId,
            title = "테스트 결정",
            optionA = "선택지 A",
            optionB = "선택지 B",
            priorityAxis = null,
            result = result,
            createdAt = Instant.now()
        )
    }

    private fun createMockFragment(id: FragmentId): ThoughtFragment {
        return ThoughtFragment(
            id = id,
            userId = userId,
            textRaw = "테스트 생각 파편",
            createdAt = Instant.now(),
            moodValence = MoodValence(0.5),
            arousal = Arousal(0.5),
            topicHint = null,
            embedding = Embedding(FloatArray(1536) { 0.1f }),
            deletedAt = null
        )
    }
}
