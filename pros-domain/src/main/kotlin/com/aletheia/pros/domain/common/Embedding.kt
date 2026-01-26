package com.aletheia.pros.domain.common

/**
 * Value object representing a vector embedding.
 *
 * Embeddings are used for:
 * - Semantic similarity search between fragments
 * - Mapping fragments to value axes
 * - Decision context matching
 *
 * @property values The embedding vector (typically 1536 dimensions for OpenAI)
 */
@JvmInline
value class Embedding(val values: FloatArray) {

    init {
        require(values.isNotEmpty()) { "Embedding cannot be empty" }
    }

    val dimension: Int get() = values.size

    /**
     * Calculates cosine similarity with another embedding.
     *
     * @return Similarity score between -1.0 and 1.0
     */
    fun cosineSimilarity(other: Embedding): Double {
        require(this.dimension == other.dimension) {
            "Embeddings must have same dimension: ${this.dimension} vs ${other.dimension}"
        }

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in values.indices) {
            dotProduct += values[i] * other.values[i]
            normA += values[i] * values[i]
            normB += other.values[i] * other.values[i]
        }

        val denominator = kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB)
        return if (denominator == 0.0) 0.0 else dotProduct / denominator
    }

    /**
     * Calculates dot product with another embedding.
     */
    fun dotProduct(other: Embedding): Double {
        require(this.dimension == other.dimension) {
            "Embeddings must have same dimension"
        }

        return values.indices.sumOf { i ->
            (values[i] * other.values[i]).toDouble()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Embedding) return false
        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int = values.contentHashCode()

    companion object {
        /**
         * Creates an embedding from a list of doubles.
         */
        fun fromDoubles(values: List<Double>): Embedding =
            Embedding(values.map { it.toFloat() }.toFloatArray())

        /**
         * Creates a zero embedding of the specified dimension.
         */
        fun zeros(dimension: Int): Embedding =
            Embedding(FloatArray(dimension) { 0f })
    }
}
