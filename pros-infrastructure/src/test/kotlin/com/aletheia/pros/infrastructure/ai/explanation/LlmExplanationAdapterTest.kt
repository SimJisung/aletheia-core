package com.aletheia.pros.infrastructure.ai.explanation

import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.Decision
import com.aletheia.pros.domain.decision.DecisionResult
import com.aletheia.pros.domain.decision.Probability
import com.aletheia.pros.domain.decision.RegretRisk
import com.aletheia.pros.domain.fragment.Arousal
import com.aletheia.pros.domain.fragment.MoodValence
import com.aletheia.pros.domain.fragment.ThoughtFragment
import com.aletheia.pros.domain.common.Embedding
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.ai.chat.client.ChatClient
import java.time.Instant

@ExtendWith(MockKExtension::class)
@DisplayName("LlmExplanationAdapter Tests")
class LlmExplanationAdapterTest {

    @MockK
    private lateinit var chatClientBuilder: ChatClient.Builder

    @MockK
    private lateinit var chatClient: ChatClient

    @MockK
    private lateinit var promptRequest: ChatClient.ChatClientRequest

    @MockK
    private lateinit var callResponse: ChatClient.CallResponse

    private lateinit var adapter: LlmExplanationAdapter

    @BeforeEach
    fun setUp() {
        every { chatClientBuilder.build() } returns chatClient
        adapter = LlmExplanationAdapter(chatClientBuilder, "")
    }

    private fun setupChatClientResponse(responseContent: String) {
        every { chatClient.prompt(any()) } returns promptRequest
        every { promptRequest.call() } returns callResponse
        every { callResponse.content() } returns responseContent
    }

    private fun createTestDecision(): Decision {
        return Decision(
            id = DecisionId.generate(),
            userId = UserId.generate(),
            title = "이직 결정",
            optionA = "현재 회사에 남기",
            optionB = "새 회사로 이직하기",
            priorityAxis = null,
            result = DecisionResult(
                probabilityA = Probability(0.45),
                probabilityB = Probability(0.55),
                regretRiskA = RegretRisk(0.3),
                regretRiskB = RegretRisk(0.25),
                evidenceFragmentIds = listOf(FragmentId.generate()),
                valueAlignment = emptyMap()
            ),
            createdAt = Instant.now()
        )
    }

    private fun createTestFragments(count: Int = 3): List<ThoughtFragment> {
        return (1..count).map { index ->
            ThoughtFragment.create(
                userId = UserId.generate(),
                textRaw = "테스트 생각 파편 $index - 여기에 사용자의 생각이 들어갑니다.",
                moodValence = MoodValence(0.5),
                arousal = Arousal(0.5)
            )
        }
    }

    @Nested
    @DisplayName("explainDecision - Successful Explanation")
    inner class SuccessfulExplanation {

        @Test
        fun `should parse explanation response with all sections`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val fragments = createTestFragments()
            val responseText = """
                [요약]
                과거 기록에 따르면 B 선택지가 약간 더 높은 적합도를 보입니다.

                [근거]
                사용자의 과거 생각 파편들에서 변화에 대한 긍정적 기록이 있습니다.

                [가치]
                이 결정은 성장과 안정 가치 사이의 선택을 반영합니다.
            """.trimIndent()
            setupChatClientResponse(responseText)

            // When
            val result = adapter.explainDecision(decision, fragments)

            // Then
            assertThat(result.summary).contains("B 선택지")
            assertThat(result.evidenceSummary).contains("생각 파편")
            assertThat(result.valueSummary).contains("가치")
        }

        @Test
        fun `should call chat client with proper prompt`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val fragments = createTestFragments()
            setupChatClientResponse("[요약]\n테스트 요약\n[근거]\n테스트 근거\n[가치]\n테스트 가치")

            // When
            adapter.explainDecision(decision, fragments)

            // Then
            verify(exactly = 1) { chatClient.prompt(any()) }
        }

        @Test
        fun `should handle response without section markers`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val fragments = createTestFragments()
            val responseText = "이 결정은 과거 패턴을 기반으로 계산되었습니다."
            setupChatClientResponse(responseText)

            // When
            val result = adapter.explainDecision(decision, fragments)

            // Then
            assertThat(result.summary).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("explainDecision - Error Handling")
    inner class ExplainDecisionErrorHandling {

        @Test
        fun `should return default explanation on API failure`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val fragments = createTestFragments()
            every { chatClient.prompt(any()) } throws RuntimeException("API error")

            // When
            val result = adapter.explainDecision(decision, fragments)

            // Then
            assertThat(result.summary).contains("계산되었습니다")
            assertThat(result.evidenceSummary).contains("기반으로")
            assertThat(result.valueSummary).isNotEmpty()
        }

        @Test
        fun `should return default explanation on null response`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val fragments = createTestFragments()
            every { chatClient.prompt(any()) } returns promptRequest
            every { promptRequest.call() } returns callResponse
            every { callResponse.content() } returns null

            // When
            val result = adapter.explainDecision(decision, fragments)

            // Then
            assertThat(result.summary).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("explainDecision - Section Parsing")
    inner class SectionParsing {

        @Test
        fun `should extract summary section correctly`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val fragments = createTestFragments()
            val responseText = """
                [요약]
                첫 번째 요약 줄입니다.
                두 번째 요약 줄입니다.

                [근거]
                근거 내용
            """.trimIndent()
            setupChatClientResponse(responseText)

            // When
            val result = adapter.explainDecision(decision, fragments)

            // Then
            assertThat(result.summary).contains("첫 번째 요약")
            assertThat(result.summary).contains("두 번째 요약")
        }

        @Test
        fun `should handle missing sections with defaults`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val fragments = createTestFragments()
            val responseText = """
                [요약]
                요약만 있음
            """.trimIndent()
            setupChatClientResponse(responseText)

            // When
            val result = adapter.explainDecision(decision, fragments)

            // Then
            assertThat(result.summary).contains("요약만 있음")
            assertThat(result.evidenceSummary).isNotEmpty() // should have default
            assertThat(result.valueSummary).isNotEmpty()    // should have default
        }
    }

    @Nested
    @DisplayName("summarizeFragments - Fragment Summarization")
    inner class SummarizeFragments {

        @Test
        fun `should return message for empty fragments`() = runBlocking {
            // When
            val result = adapter.summarizeFragments(emptyList())

            // Then
            assertThat(result).isEqualTo("기록된 생각 파편이 없습니다.")
        }

        @Test
        fun `should call chat client for non-empty fragments`() = runBlocking {
            // Given
            val fragments = createTestFragments(5)
            setupChatClientResponse("요약된 내용입니다.")

            // When
            val result = adapter.summarizeFragments(fragments)

            // Then
            assertThat(result).isEqualTo("요약된 내용입니다.")
            verify(exactly = 1) { chatClient.prompt(any()) }
        }

        @Test
        fun `should return default message on API failure`() = runBlocking {
            // Given
            val fragments = createTestFragments()
            every { chatClient.prompt(any()) } throws RuntimeException("API error")

            // When
            val result = adapter.summarizeFragments(fragments)

            // Then
            assertThat(result).isEqualTo("요약을 생성할 수 없습니다.")
        }

        @Test
        fun `should return default message on null response`() = runBlocking {
            // Given
            val fragments = createTestFragments()
            every { chatClient.prompt(any()) } returns promptRequest
            every { promptRequest.call() } returns callResponse
            every { callResponse.content() } returns null

            // When
            val result = adapter.summarizeFragments(fragments)

            // Then
            assertThat(result).isEqualTo("요약을 생성할 수 없습니다.")
        }

        @Test
        fun `should limit fragments to 10 for summarization`() = runBlocking {
            // Given
            val fragments = createTestFragments(15)
            setupChatClientResponse("요약 결과")

            // When
            adapter.summarizeFragments(fragments)

            // Then
            verify(exactly = 1) { chatClient.prompt(any()) }
            // Only 10 fragments should be included in the prompt
        }
    }

    @Nested
    @DisplayName("System Prompt Configuration")
    inner class SystemPromptConfiguration {

        @Test
        fun `should use configured system prompt when provided`() = runBlocking {
            // Given
            val customPrompt = "Custom system prompt for testing"
            every { chatClientBuilder.build() } returns chatClient
            val customAdapter = LlmExplanationAdapter(chatClientBuilder, customPrompt)

            val decision = createTestDecision()
            val fragments = createTestFragments()
            setupChatClientResponse("[요약]\n테스트\n[근거]\n테스트\n[가치]\n테스트")

            // When
            customAdapter.explainDecision(decision, fragments)

            // Then
            verify { chatClient.prompt(any()) }
        }

        @Test
        fun `should use default system prompt when not configured`() = runBlocking {
            // Given
            every { chatClientBuilder.build() } returns chatClient
            val defaultAdapter = LlmExplanationAdapter(chatClientBuilder, "")

            val decision = createTestDecision()
            val fragments = createTestFragments()
            setupChatClientResponse("[요약]\n테스트\n[근거]\n테스트\n[가치]\n테스트")

            // When
            defaultAdapter.explainDecision(decision, fragments)

            // Then
            verify { chatClient.prompt(any()) }
        }
    }

    @Nested
    @DisplayName("No Recommendation Guardrails")
    inner class NoRecommendationGuardrails {

        @Test
        fun `default explanation should not contain recommendation language`() = runBlocking {
            // Given
            val decision = createTestDecision()
            val fragments = createTestFragments()
            every { chatClient.prompt(any()) } throws RuntimeException("API error")

            // When
            val result = adapter.explainDecision(decision, fragments)

            // Then
            assertThat(result.summary.lowercase()).doesNotContain("should")
            assertThat(result.summary.lowercase()).doesNotContain("recommend")
            assertThat(result.summary.lowercase()).doesNotContain("better")
            assertThat(result.summary).doesNotContain("추천")
            assertThat(result.summary).doesNotContain("해야")
        }
    }
}
