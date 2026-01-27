package com.aletheia.pros.application.usecase.decision

import com.aletheia.pros.application.port.output.EmbeddingPort
import com.aletheia.pros.domain.common.Embedding
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.Decision
import com.aletheia.pros.domain.decision.DecisionRepository
import com.aletheia.pros.domain.fragment.FragmentRepository
import com.aletheia.pros.domain.fragment.ThoughtFragment
import com.aletheia.pros.domain.value.ValueAxis
import com.aletheia.pros.domain.value.ValueGraph
import com.aletheia.pros.domain.value.ValueGraphRepository
import com.aletheia.pros.domain.value.ValueNode
import io.mockk.*
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
@DisplayName("CreateDecisionUseCase Tests")
class CreateDecisionUseCaseTest {

    @MockK
    private lateinit var decisionRepository: DecisionRepository

    @MockK
    private lateinit var fragmentRepository: FragmentRepository

    @MockK
    private lateinit var valueGraphRepository: ValueGraphRepository

    @MockK
    private lateinit var embeddingPort: EmbeddingPort

    @MockK
    private lateinit var userSettingsProvider: UserSettingsProvider

    private lateinit var useCase: CreateDecisionUseCase

    private val userId = UserId.generate()
    private val testEmbedding = Embedding(FloatArray(1536) { 0.1f })

    @BeforeEach
    fun setUp() {
        useCase = CreateDecisionUseCase(
            decisionRepository = decisionRepository,
            fragmentRepository = fragmentRepository,
            valueGraphRepository = valueGraphRepository,
            embeddingPort = embeddingPort,
            userSettingsProvider = userSettingsProvider
        )
    }

    @Nested
    @DisplayName("Decision Projection Calculation")
    inner class DecisionProjection {

        @Test
        fun `should create decision with probability based on similar fragments`() = runBlocking {
            // Given
            val command = CreateDecisionCommand(
                userId = userId,
                title = "이직 결정",
                optionA = "현재 회사 유지",
                optionB = "새 회사로 이직",
                priorityAxis = null
            )

            // Mock similar fragments - more positive ones for option A context
            val fragments = createMockFragments()

            coEvery { embeddingPort.embed(any()) } returns testEmbedding
            coEvery { fragmentRepository.findSimilarByEmbedding(userId, testEmbedding, any(), any()) } returns fragments
            coEvery { valueGraphRepository.findValueGraph(userId) } returns createMockValueGraph()
            coEvery { userSettingsProvider.getSettings(userId) } returns UserSettings(lambda = 1.0, regretPrior = 0.2)
            coEvery { decisionRepository.save(any()) } answers { firstArg() }

            // When
            val result = useCase.execute(command)

            // Then
            assertThat(result.title).isEqualTo("이직 결정")
            assertThat(result.optionA).isEqualTo("현재 회사 유지")
            assertThat(result.optionB).isEqualTo("새 회사로 이직")

            // Probabilities should sum to 1
            val totalProb = result.result.probabilityA.value + result.result.probabilityB.value
            assertThat(totalProb).isEqualTo(1.0, org.assertj.core.data.Offset.offset(0.001))

            // Evidence fragments should be recorded
            assertThat(result.result.evidenceFragmentIds).isNotEmpty

            coVerify(exactly = 1) { decisionRepository.save(any()) }
        }

        @Test
        fun `should apply priority axis weight when specified`() = runBlocking {
            // Given
            val command = CreateDecisionCommand(
                userId = userId,
                title = "휴가 계획",
                optionA = "여행 가기",
                optionB = "집에서 쉬기",
                priorityAxis = ValueAxis.FINANCIAL
            )

            val fragments = createMockFragments()
            val valueGraph = createMockValueGraph()

            coEvery { embeddingPort.embed(any()) } returns testEmbedding
            coEvery { fragmentRepository.findSimilarByEmbedding(any(), any(), any(), any()) } returns fragments
            coEvery { valueGraphRepository.findValueGraph(userId) } returns valueGraph
            coEvery { userSettingsProvider.getSettings(userId) } returns UserSettings(lambda = 1.0, regretPrior = 0.2)
            coEvery { decisionRepository.save(any()) } answers { firstArg() }

            // When
            val result = useCase.execute(command)

            // Then
            assertThat(result.priorityAxis).isEqualTo(ValueAxis.FINANCIAL)
            assertThat(result.result.valueAlignment).containsKey(ValueAxis.FINANCIAL)
        }

        @Test
        fun `should handle no similar fragments gracefully`() = runBlocking {
            // Given
            val command = CreateDecisionCommand(
                userId = userId,
                title = "새로운 결정",
                optionA = "옵션 A",
                optionB = "옵션 B",
                priorityAxis = null
            )

            coEvery { embeddingPort.embed(any()) } returns testEmbedding
            coEvery { fragmentRepository.findSimilarByEmbedding(any(), any(), any(), any()) } returns emptyList()
            coEvery { valueGraphRepository.findValueGraph(userId) } returns null
            coEvery { userSettingsProvider.getSettings(userId) } returns UserSettings(lambda = 1.0, regretPrior = 0.2)
            coEvery { decisionRepository.save(any()) } answers { firstArg() }

            // When
            val result = useCase.execute(command)

            // Then - should return roughly 50/50 when no evidence
            assertThat(result.result.probabilityA.value).isEqualTo(0.5, org.assertj.core.data.Offset.offset(0.1))
            assertThat(result.result.probabilityB.value).isEqualTo(0.5, org.assertj.core.data.Offset.offset(0.1))
            assertThat(result.result.evidenceFragmentIds).isEmpty()
        }
    }

    @Nested
    @DisplayName("Regret Risk Calculation")
    inner class RegretRiskCalculation {

        @Test
        fun `should calculate regret risk based on lambda and prior`() = runBlocking {
            // Given
            val command = CreateDecisionCommand(
                userId = userId,
                title = "테스트 결정",
                optionA = "A",
                optionB = "B",
                priorityAxis = null
            )

            // High lambda = more sensitive to regret
            coEvery { embeddingPort.embed(any()) } returns testEmbedding
            coEvery { fragmentRepository.findSimilarByEmbedding(any(), any(), any(), any()) } returns createMockFragments()
            coEvery { valueGraphRepository.findValueGraph(userId) } returns createMockValueGraph()
            coEvery { userSettingsProvider.getSettings(userId) } returns UserSettings(lambda = 2.0, regretPrior = 0.3)
            coEvery { decisionRepository.save(any()) } answers { firstArg() }

            // When
            val result = useCase.execute(command)

            // Then
            assertThat(result.result.regretRiskA.value).isBetween(0.0, 1.0)
            assertThat(result.result.regretRiskB.value).isBetween(0.0, 1.0)

            // Lower probability option should have higher regret risk
            if (result.result.probabilityA.value < result.result.probabilityB.value) {
                assertThat(result.result.regretRiskA.value)
                    .isGreaterThanOrEqualTo(result.result.regretRiskB.value)
            }
        }
    }

    @Nested
    @DisplayName("Design Principles Verification")
    inner class DesignPrinciples {

        @Test
        fun `should not include recommendations in result - only probabilities`() = runBlocking {
            // Given
            val command = CreateDecisionCommand(
                userId = userId,
                title = "중요한 결정",
                optionA = "선택 A",
                optionB = "선택 B",
                priorityAxis = null
            )

            coEvery { embeddingPort.embed(any()) } returns testEmbedding
            coEvery { fragmentRepository.findSimilarByEmbedding(any(), any(), any(), any()) } returns createMockFragments()
            coEvery { valueGraphRepository.findValueGraph(userId) } returns createMockValueGraph()
            coEvery { userSettingsProvider.getSettings(userId) } returns UserSettings(lambda = 1.0, regretPrior = 0.2)
            coEvery { decisionRepository.save(any()) } answers { firstArg() }

            // When
            val result = useCase.execute(command)

            // Then - Decision result should contain only probabilities, not recommendations
            // This verifies the design principle: "LLM은 설명만, 판단은 안함"
            assertThat(result.result.probabilityA).isNotNull
            assertThat(result.result.probabilityB).isNotNull
            assertThat(result.result.evidenceFragmentIds).isNotNull

            // The Decision class should not have recommendation fields
            // (verified by compile-time - no such property exists)
        }
    }

    // Helper methods
    private fun createMockFragments(): List<ThoughtFragment> {
        return listOf(
            ThoughtFragment.create(
                userId = userId,
                textRaw = "새로운 도전을 좋아한다",
                moodValence = 0.7,
                arousal = 0.6,
                embedding = testEmbedding
            ),
            ThoughtFragment.create(
                userId = userId,
                textRaw = "안정이 중요하다고 느꼈다",
                moodValence = 0.5,
                arousal = 0.3,
                embedding = testEmbedding
            )
        )
    }

    private fun createMockValueGraph(): ValueGraph {
        val nodes = ValueAxis.all().map { axis ->
            ValueNode.createInitial(userId, axis)
        }
        return ValueGraph(userId, nodes, emptyList())
    }
}
