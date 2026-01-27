package com.aletheia.pros.application.usecase.fragment

import com.aletheia.pros.application.port.output.EmbeddingPort
import com.aletheia.pros.application.port.output.EmotionAnalysisPort
import com.aletheia.pros.application.port.output.EmotionAnalysisResult
import com.aletheia.pros.domain.common.Embedding
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.fragment.FragmentRepository
import com.aletheia.pros.domain.fragment.ThoughtFragment
import io.mockk.*
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

@ExtendWith(MockKExtension::class)
@DisplayName("CreateFragmentUseCase Tests")
class CreateFragmentUseCaseTest {

    @MockK
    private lateinit var fragmentRepository: FragmentRepository

    @MockK
    private lateinit var emotionAnalysisPort: EmotionAnalysisPort

    @MockK
    private lateinit var embeddingPort: EmbeddingPort

    private lateinit var useCase: CreateFragmentUseCase

    private val userId = UserId.generate()
    private val testEmbedding = Embedding(FloatArray(1536) { 0.1f })

    @BeforeEach
    fun setUp() {
        useCase = CreateFragmentUseCase(
            fragmentRepository = fragmentRepository,
            emotionAnalysisPort = emotionAnalysisPort,
            embeddingPort = embeddingPort
        )
    }

    @Nested
    @DisplayName("Successful Fragment Creation")
    inner class SuccessfulCreation {

        @Test
        fun `should create fragment with analyzed emotion and embedding`() = runBlocking {
            // Given
            val command = CreateFragmentCommand(
                userId = userId,
                text = "오늘 정말 기분 좋은 하루였다"
            )

            val emotionResult = EmotionAnalysisResult(
                valence = 0.8,
                arousal = 0.6
            )

            coEvery { emotionAnalysisPort.analyze(command.text) } returns emotionResult
            coEvery { embeddingPort.embed(command.text) } returns testEmbedding
            coEvery { fragmentRepository.save(any()) } answers { firstArg() }

            // When
            val result = useCase.execute(command)

            // Then
            assertThat(result.userId).isEqualTo(userId)
            assertThat(result.textRaw).isEqualTo("오늘 정말 기분 좋은 하루였다")
            assertThat(result.moodValence.value).isEqualTo(0.8)
            assertThat(result.arousal.value).isEqualTo(0.6)
            assertThat(result.embedding).isEqualTo(testEmbedding)

            coVerify(exactly = 1) { emotionAnalysisPort.analyze(command.text) }
            coVerify(exactly = 1) { embeddingPort.embed(command.text) }
            coVerify(exactly = 1) { fragmentRepository.save(any()) }
        }

        @Test
        fun `should preserve original text without modification`() = runBlocking {
            // Given
            val originalText = "  복잡한 감정이 드는 순간...  "
            val command = CreateFragmentCommand(userId = userId, text = originalText)

            coEvery { emotionAnalysisPort.analyze(any()) } returns EmotionAnalysisResult(0.0, 0.5)
            coEvery { embeddingPort.embed(any()) } returns testEmbedding
            coEvery { fragmentRepository.save(any()) } answers { firstArg() }

            // When
            val result = useCase.execute(command)

            // Then - text should be preserved as-is (append-only principle)
            assertThat(result.textRaw).isEqualTo(originalText)
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        fun `should fail when emotion analysis fails`(): Unit = runBlocking {
            // Given
            val command = CreateFragmentCommand(userId = userId, text = "Test text")

            coEvery { emotionAnalysisPort.analyze(any()) } throws RuntimeException("LLM error")

            // When/Then
            assertThatThrownBy {
                runBlocking { useCase.execute(command) }
            }.isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("LLM error")

            coVerify(exactly = 0) { fragmentRepository.save(any()) }
        }

        @Test
        fun `should fail when embedding generation fails`(): Unit = runBlocking {
            // Given
            val command = CreateFragmentCommand(userId = userId, text = "Test text")

            coEvery { emotionAnalysisPort.analyze(any()) } returns EmotionAnalysisResult(0.5, 0.5)
            coEvery { embeddingPort.embed(any()) } throws RuntimeException("Embedding error")

            // When/Then
            assertThatThrownBy {
                runBlocking { useCase.execute(command) }
            }.isInstanceOf(RuntimeException::class.java)

            coVerify(exactly = 0) { fragmentRepository.save(any()) }
        }

        @Test
        fun `should fail when repository save fails`(): Unit = runBlocking {
            // Given
            val command = CreateFragmentCommand(userId = userId, text = "Test text")

            coEvery { emotionAnalysisPort.analyze(any()) } returns EmotionAnalysisResult(0.5, 0.5)
            coEvery { embeddingPort.embed(any()) } returns testEmbedding
            coEvery { fragmentRepository.save(any()) } throws RuntimeException("DB error")

            // When/Then
            assertThatThrownBy {
                runBlocking { useCase.execute(command) }
            }.isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("DB error")
        }
    }

    @Nested
    @DisplayName("Validation")
    inner class Validation {

        @Test
        fun `should reject empty text`() {
            // Given
            val command = CreateFragmentCommand(userId = userId, text = "")

            // When/Then
            assertThatThrownBy {
                runBlocking { useCase.execute(command) }
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `should reject blank text`() {
            // Given
            val command = CreateFragmentCommand(userId = userId, text = "   ")

            // When/Then
            assertThatThrownBy {
                runBlocking { useCase.execute(command) }
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
