package com.aletheia.pros.infrastructure.ai.emotion

import com.aletheia.pros.domain.fragment.Arousal
import com.aletheia.pros.domain.fragment.MoodValence
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.ai.chat.client.ChatClient

@ExtendWith(MockKExtension::class)
@DisplayName("LlmEmotionAnalysisAdapter Tests")
class LlmEmotionAnalysisAdapterTest {

    @MockK
    private lateinit var chatClientBuilder: ChatClient.Builder

    @MockK
    private lateinit var chatClient: ChatClient

    @MockK
    private lateinit var promptRequest: ChatClient.ChatClientRequest

    @MockK
    private lateinit var callResponse: ChatClient.CallResponse

    private lateinit var adapter: LlmEmotionAnalysisAdapter

    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        every { chatClientBuilder.build() } returns chatClient
        adapter = LlmEmotionAnalysisAdapter(chatClientBuilder, objectMapper)
    }

    private fun setupChatClientResponse(responseContent: String) {
        every { chatClient.prompt(any()) } returns promptRequest
        every { promptRequest.call() } returns callResponse
        every { callResponse.content() } returns responseContent
    }

    @Nested
    @DisplayName("analyzeEmotion - Successful Analysis")
    inner class SuccessfulAnalysis {

        @Test
        fun `should parse positive emotion response correctly`() = runBlocking {
            // Given
            val text = "오늘 정말 기분 좋은 하루였다!"
            val responseJson = """{"valence": 0.8, "arousal": 0.7, "confidence": 0.9}"""
            setupChatClientResponse(responseJson)

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.valence.value).isEqualTo(0.8)
            assertThat(result.arousal.value).isEqualTo(0.7)
            assertThat(result.confidence).isEqualTo(0.9)
        }

        @Test
        fun `should parse negative emotion response correctly`() = runBlocking {
            // Given
            val text = "정말 화가 난다"
            val responseJson = """{"valence": -0.7, "arousal": 0.8, "confidence": 0.85}"""
            setupChatClientResponse(responseJson)

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.valence.value).isEqualTo(-0.7)
            assertThat(result.valence.isNegative).isTrue()
            assertThat(result.arousal.isHigh).isTrue()
        }

        @Test
        fun `should parse neutral emotion response correctly`() = runBlocking {
            // Given
            val text = "점심을 먹었다"
            val responseJson = """{"valence": 0.1, "arousal": 0.3, "confidence": 0.7}"""
            setupChatClientResponse(responseJson)

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.valence.value).isEqualTo(0.1)
            assertThat(result.arousal.value).isEqualTo(0.3)
            assertThat(result.arousal.isLow).isTrue()
        }

        @Test
        fun `should call chat client with prompt`() = runBlocking {
            // Given
            val text = "Test text"
            setupChatClientResponse("""{"valence": 0.5, "arousal": 0.5, "confidence": 0.8}""")

            // When
            adapter.analyzeEmotion(text)

            // Then
            verify(exactly = 1) { chatClient.prompt(any()) }
            verify(exactly = 1) { promptRequest.call() }
        }
    }

    @Nested
    @DisplayName("analyzeEmotion - Value Coercion")
    inner class ValueCoercion {

        @Test
        fun `should coerce valence above 1_0 to 1_0`() = runBlocking {
            // Given
            val text = "Test"
            val responseJson = """{"valence": 1.5, "arousal": 0.5, "confidence": 0.8}"""
            setupChatClientResponse(responseJson)

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.valence.value).isEqualTo(1.0)
        }

        @Test
        fun `should coerce valence below -1_0 to -1_0`() = runBlocking {
            // Given
            val text = "Test"
            val responseJson = """{"valence": -1.5, "arousal": 0.5, "confidence": 0.8}"""
            setupChatClientResponse(responseJson)

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.valence.value).isEqualTo(-1.0)
        }

        @Test
        fun `should coerce arousal above 1_0 to 1_0`() = runBlocking {
            // Given
            val text = "Test"
            val responseJson = """{"valence": 0.5, "arousal": 1.5, "confidence": 0.8}"""
            setupChatClientResponse(responseJson)

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.arousal.value).isEqualTo(1.0)
        }

        @Test
        fun `should coerce arousal below 0_0 to 0_0`() = runBlocking {
            // Given
            val text = "Test"
            val responseJson = """{"valence": 0.5, "arousal": -0.5, "confidence": 0.8}"""
            setupChatClientResponse(responseJson)

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.arousal.value).isEqualTo(0.0)
        }

        @Test
        fun `should coerce confidence to valid range`() = runBlocking {
            // Given
            val text = "Test"
            val responseJson = """{"valence": 0.5, "arousal": 0.5, "confidence": 1.5}"""
            setupChatClientResponse(responseJson)

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.confidence).isEqualTo(1.0)
        }
    }

    @Nested
    @DisplayName("analyzeEmotion - Error Handling")
    inner class ErrorHandling {

        @Test
        fun `should return neutral defaults on API failure`() = runBlocking {
            // Given
            val text = "Test"
            every { chatClient.prompt(any()) } throws RuntimeException("API error")

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.valence).isEqualTo(MoodValence.NEUTRAL)
            assertThat(result.arousal).isEqualTo(Arousal.MODERATE)
            assertThat(result.confidence).isEqualTo(0.0)
        }

        @Test
        fun `should return neutral defaults on null response`() = runBlocking {
            // Given
            val text = "Test"
            every { chatClient.prompt(any()) } returns promptRequest
            every { promptRequest.call() } returns callResponse
            every { callResponse.content() } returns null

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.valence).isEqualTo(MoodValence.NEUTRAL)
            assertThat(result.arousal).isEqualTo(Arousal.MODERATE)
            assertThat(result.confidence).isEqualTo(0.0)
        }

        @Test
        fun `should return neutral defaults on invalid JSON response`() = runBlocking {
            // Given
            val text = "Test"
            setupChatClientResponse("This is not valid JSON")

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.valence).isEqualTo(MoodValence.NEUTRAL)
            assertThat(result.arousal).isEqualTo(Arousal.MODERATE)
            assertThat(result.confidence).isEqualTo(0.0)
        }

        @Test
        fun `should return neutral defaults on incomplete JSON response`() = runBlocking {
            // Given
            val text = "Test"
            setupChatClientResponse("""{"valence": 0.5}""")  // missing arousal and confidence

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.valence.value).isEqualTo(0.5)
            assertThat(result.arousal.value).isEqualTo(0.5) // default
            assertThat(result.confidence).isEqualTo(0.5)    // default
        }

        @Test
        fun `should handle empty JSON response`() = runBlocking {
            // Given
            val text = "Test"
            setupChatClientResponse("{}")

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.valence.value).isEqualTo(0.0) // default
            assertThat(result.arousal.value).isEqualTo(0.5) // default
            assertThat(result.confidence).isEqualTo(0.5)    // default
        }
    }

    @Nested
    @DisplayName("analyzeEmotion - JSON Parsing Edge Cases")
    inner class JsonParsingEdgeCases {

        @Test
        fun `should handle JSON with extra whitespace`() = runBlocking {
            // Given
            val text = "Test"
            val responseJson = """
                {
                    "valence": 0.5,
                    "arousal": 0.6,
                    "confidence": 0.7
                }
            """
            setupChatClientResponse(responseJson)

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.valence.value).isEqualTo(0.5)
            assertThat(result.arousal.value).isEqualTo(0.6)
            assertThat(result.confidence).isEqualTo(0.7)
        }

        @Test
        fun `should handle JSON with extra fields`() = runBlocking {
            // Given
            val text = "Test"
            val responseJson = """{"valence": 0.5, "arousal": 0.6, "confidence": 0.7, "extra": "field"}"""
            setupChatClientResponse(responseJson)

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.valence.value).isEqualTo(0.5)
            assertThat(result.arousal.value).isEqualTo(0.6)
            assertThat(result.confidence).isEqualTo(0.7)
        }

        @Test
        fun `should handle integer values in JSON`() = runBlocking {
            // Given
            val text = "Test"
            val responseJson = """{"valence": 1, "arousal": 0, "confidence": 1}"""
            setupChatClientResponse(responseJson)

            // When
            val result = adapter.analyzeEmotion(text)

            // Then
            assertThat(result.valence.value).isEqualTo(1.0)
            assertThat(result.arousal.value).isEqualTo(0.0)
            assertThat(result.confidence).isEqualTo(1.0)
        }
    }
}
