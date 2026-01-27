package com.aletheia.pros.infrastructure.ai.embedding

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.ai.embedding.EmbeddingResultMetadata

@ExtendWith(MockKExtension::class)
@DisplayName("OpenAiEmbeddingAdapter Tests")
class OpenAiEmbeddingAdapterTest {

    @MockK
    private lateinit var embeddingModel: EmbeddingModel

    private lateinit var adapter: OpenAiEmbeddingAdapter

    @BeforeEach
    fun setUp() {
        adapter = OpenAiEmbeddingAdapter(embeddingModel)
    }

    private fun createMockEmbedding(size: Int = 1536): List<Double> {
        return (0 until size).map { it * 0.001 }
    }

    private fun createMockEmbeddingResponse(vararg embeddings: List<Double>): EmbeddingResponse {
        val results = embeddings.mapIndexed { index, values ->
            Embedding(values, index, EmbeddingResultMetadata.EMPTY)
        }
        return EmbeddingResponse(results)
    }

    @Nested
    @DisplayName("embed - Single Text Embedding")
    inner class SingleEmbed {

        @Test
        fun `should generate embedding for text`() = runBlocking {
            // Given
            val text = "This is a test text for embedding"
            val mockEmbeddingValues = createMockEmbedding()
            val mockResponse = createMockEmbeddingResponse(mockEmbeddingValues)

            every { embeddingModel.call(any<EmbeddingRequest>()) } returns mockResponse

            // When
            val result = adapter.embed(text)

            // Then
            assertThat(result.values).hasSize(1536)
            verify(exactly = 1) { embeddingModel.call(any<EmbeddingRequest>()) }
        }

        @Test
        fun `should return correct dimension embedding`() = runBlocking {
            // Given
            val text = "Test"
            val mockEmbeddingValues = createMockEmbedding(1536)
            val mockResponse = createMockEmbeddingResponse(mockEmbeddingValues)

            every { embeddingModel.call(any<EmbeddingRequest>()) } returns mockResponse

            // When
            val result = adapter.embed(text)

            // Then
            assertThat(result.values.size).isEqualTo(adapter.embeddingDimension)
        }

        @Test
        fun `should convert double values to float array`() = runBlocking {
            // Given
            val text = "Test"
            val mockEmbeddingValues = listOf(0.1, 0.2, 0.3)
            val mockResponse = EmbeddingResponse(
                listOf(Embedding(mockEmbeddingValues, 0, EmbeddingResultMetadata.EMPTY))
            )

            every { embeddingModel.call(any<EmbeddingRequest>()) } returns mockResponse

            // When
            val result = adapter.embed(text)

            // Then
            assertThat(result.values[0]).isEqualTo(0.1f)
            assertThat(result.values[1]).isEqualTo(0.2f)
            assertThat(result.values[2]).isEqualTo(0.3f)
        }

        @Test
        fun `should propagate exceptions from embedding model`() {
            // Given
            val text = "Test"

            every { embeddingModel.call(any<EmbeddingRequest>()) } throws RuntimeException("API error")

            // When/Then
            assertThatThrownBy {
                runBlocking { adapter.embed(text) }
            }.isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("API error")
        }
    }

    @Nested
    @DisplayName("embedBatch - Batch Embedding")
    inner class BatchEmbed {

        @Test
        fun `should generate embeddings for multiple texts`() = runBlocking {
            // Given
            val texts = listOf("Text 1", "Text 2", "Text 3")
            val mockResponse = createMockEmbeddingResponse(
                createMockEmbedding(),
                createMockEmbedding(),
                createMockEmbedding()
            )

            every { embeddingModel.call(any<EmbeddingRequest>()) } returns mockResponse

            // When
            val results = adapter.embedBatch(texts)

            // Then
            assertThat(results).hasSize(3)
            results.forEach { embedding ->
                assertThat(embedding.values).hasSize(1536)
            }
        }

        @Test
        fun `should return empty list for empty input`() = runBlocking {
            // When
            val results = adapter.embedBatch(emptyList())

            // Then
            assertThat(results).isEmpty()
        }

        @Test
        fun `should preserve order of embeddings`() = runBlocking {
            // Given
            val texts = listOf("First", "Second", "Third")
            val embedding1 = (0 until 1536).map { 0.1 }
            val embedding2 = (0 until 1536).map { 0.2 }
            val embedding3 = (0 until 1536).map { 0.3 }
            val mockResponse = createMockEmbeddingResponse(embedding1, embedding2, embedding3)

            every { embeddingModel.call(any<EmbeddingRequest>()) } returns mockResponse

            // When
            val results = adapter.embedBatch(texts)

            // Then
            assertThat(results).hasSize(3)
            assertThat(results[0].values[0]).isEqualTo(0.1f)
            assertThat(results[1].values[0]).isEqualTo(0.2f)
            assertThat(results[2].values[0]).isEqualTo(0.3f)
        }

        @Test
        fun `should make single API call for batch`() = runBlocking {
            // Given
            val texts = listOf("Text 1", "Text 2")
            val mockResponse = createMockEmbeddingResponse(
                createMockEmbedding(),
                createMockEmbedding()
            )

            every { embeddingModel.call(any<EmbeddingRequest>()) } returns mockResponse

            // When
            adapter.embedBatch(texts)

            // Then - should be a single call, not multiple
            verify(exactly = 1) { embeddingModel.call(any<EmbeddingRequest>()) }
        }
    }

    @Nested
    @DisplayName("embeddingDimension - Dimension Property")
    inner class EmbeddingDimension {

        @Test
        fun `should return 1536 as embedding dimension`() {
            // When
            val dimension = adapter.embeddingDimension

            // Then
            assertThat(dimension).isEqualTo(1536)
        }

        @Test
        fun `should match OpenAI text-embedding-3-small dimension`() {
            // Then
            assertThat(adapter.embeddingDimension).isEqualTo(OpenAiEmbeddingAdapter.EMBEDDING_DIMENSION)
        }
    }
}
