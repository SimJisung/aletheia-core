package com.aletheia.pros.application.port.output

import com.aletheia.pros.domain.common.Embedding

/**
 * Output port for text embedding generation.
 *
 * Embeddings are used for:
 * - Semantic similarity search between fragments
 * - Mapping fragments to value axes
 * - Decision context matching
 *
 * Implementing adapters: OpenAI, local embedding models, etc.
 */
interface EmbeddingPort {

    /**
     * Generates an embedding for the given text.
     *
     * @param text The text to embed
     * @return The embedding vector
     */
    suspend fun embed(text: String): Embedding

    /**
     * Generates embeddings for multiple texts (batch operation).
     *
     * @param texts The texts to embed
     * @return List of embeddings in the same order as input
     */
    suspend fun embedBatch(texts: List<String>): List<Embedding>

    /**
     * Returns the dimension of embeddings produced by this port.
     */
    val embeddingDimension: Int
}
