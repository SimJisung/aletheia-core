package com.aletheia.pros.infrastructure.ai.embedding

import com.aletheia.pros.application.port.output.EmbeddingPort
import com.aletheia.pros.domain.common.Embedding
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.openai.OpenAiEmbeddingOptions
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Adapter implementing EmbeddingPort using Spring AI's OpenAI integration.
 *
 * Uses OpenAI's text-embedding-3-small model (1536 dimensions) by default.
 */
@Component
class OpenAiEmbeddingAdapter(
    private val embeddingModel: EmbeddingModel
) : EmbeddingPort {

    companion object {
        const val EMBEDDING_DIMENSION = 1536
    }

    override val embeddingDimension: Int = EMBEDDING_DIMENSION

    override suspend fun embed(text: String): Embedding {
        logger.debug { "Generating embedding for text (length=${text.length})" }

        val response = embeddingModel.call(
            EmbeddingRequest(
                listOf(text),
                OpenAiEmbeddingOptions.builder().build()
            )
        )

        val embeddingOutput = response.result.output
        val floatArray = embeddingOutput.map { it.toFloat() }.toFloatArray()

        logger.debug { "Generated embedding with dimension=${floatArray.size}" }

        return Embedding(floatArray)
    }

    override suspend fun embedBatch(texts: List<String>): List<Embedding> {
        if (texts.isEmpty()) return emptyList()

        logger.debug { "Generating embeddings for ${texts.size} texts" }

        val response = embeddingModel.call(
            EmbeddingRequest(
                texts,
                OpenAiEmbeddingOptions.builder().build()
            )
        )

        val embeddings = response.results.map { result ->
            val floatArray = result.output.map { it.toFloat() }.toFloatArray()
            Embedding(floatArray)
        }

        logger.debug { "Generated ${embeddings.size} embeddings" }

        return embeddings
    }
}
