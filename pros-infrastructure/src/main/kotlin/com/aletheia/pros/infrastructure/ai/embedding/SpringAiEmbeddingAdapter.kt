package com.aletheia.pros.infrastructure.ai.embedding

import com.aletheia.pros.application.exception.EmbeddingGenerationException
import com.aletheia.pros.application.exception.QuotaExceededException
import com.aletheia.pros.application.port.output.EmbeddingPort
import com.aletheia.pros.domain.common.Embedding
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.retry.NonTransientAiException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Adapter implementing EmbeddingPort using Spring AI's EmbeddingModel.
 *
 * Works with both OpenAI and Ollama depending on the active profile.
 */
@Component
class SpringAiEmbeddingAdapter(
    private val embeddingModel: EmbeddingModel,
    @Value("\${spring.ai.vectorstore.pgvector.dimensions:0}")
    private val configuredDimensions: Int
) : EmbeddingPort {

    override val embeddingDimension: Int = configuredDimensions

    override suspend fun embed(text: String): Embedding {
        logger.debug { "Generating embedding for text (length=${text.length})" }

        return try {
            val response = embeddingModel.call(
                EmbeddingRequest(
                    listOf(text),
                    null
                )
            )

            val embeddingOutput = response.result.output
            val floatArray = embeddingOutput.map { it.toFloat() }.toFloatArray()

            if (embeddingDimension > 0 && floatArray.size != embeddingDimension) {
                logger.warn { "Embedding dimension mismatch: expected=$embeddingDimension actual=${floatArray.size}" }
            } else {
                logger.debug { "Generated embedding with dimension=${floatArray.size}" }
            }

            Embedding(floatArray)
        } catch (e: NonTransientAiException) {
            logger.error(e) { "AI provider quota exceeded or non-retryable error" }
            if (e.message?.contains("quota", ignoreCase = true) == true ||
                e.message?.contains("insufficient_quota", ignoreCase = true) == true) {
                throw QuotaExceededException("AI provider quota exceeded", e)
            }
            throw EmbeddingGenerationException("Failed to generate embedding: ${e.message}", e)
        } catch (e: Exception) {
            logger.error(e) { "Failed to generate embedding" }
            throw EmbeddingGenerationException("Failed to generate embedding", e)
        }
    }

    override suspend fun embedBatch(texts: List<String>): List<Embedding> {
        if (texts.isEmpty()) return emptyList()

        logger.debug { "Generating embeddings for ${texts.size} texts" }

        return try {
            val response = embeddingModel.call(
                EmbeddingRequest(
                    texts,
                    null
                )
            )

            val embeddings = response.results.map { result ->
                val floatArray = result.output.map { it.toFloat() }.toFloatArray()
                Embedding(floatArray)
            }

            logger.debug { "Generated ${embeddings.size} embeddings" }

            embeddings
        } catch (e: NonTransientAiException) {
            logger.error(e) { "AI provider quota exceeded or non-retryable error in batch embedding" }
            if (e.message?.contains("quota", ignoreCase = true) == true ||
                e.message?.contains("insufficient_quota", ignoreCase = true) == true) {
                throw QuotaExceededException("AI provider quota exceeded", e)
            }
            throw EmbeddingGenerationException("Failed to generate embeddings: ${e.message}", e)
        } catch (e: Exception) {
            logger.error(e) { "Failed to generate embeddings" }
            throw EmbeddingGenerationException("Failed to generate embeddings", e)
        }
    }
}
