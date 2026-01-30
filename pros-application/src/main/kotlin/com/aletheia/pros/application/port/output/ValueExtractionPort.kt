package com.aletheia.pros.application.port.output

import com.aletheia.pros.domain.value.ValueAxis

/**
 * Output port for extracting value axes from text.
 *
 * This port analyzes text to identify which value dimensions
 * are mentioned or implied in the user's thought fragment.
 *
 * Implementation notes:
 * - Should return detected values with confidence scores
 * - May return multiple values for a single text
 * - Empty result is valid (no value detected)
 */
interface ValueExtractionPort {

    /**
     * Extracts value axes from the given text.
     *
     * @param text The text to analyze
     * @return List of detected values with confidence and sentiment
     */
    suspend fun extractValues(text: String): List<ValueExtraction>
}

/**
 * Result of value extraction from text.
 *
 * @param axis The detected value axis
 * @param confidence How confident the extraction is (0.0-1.0)
 * @param sentiment Positive (>0) or negative (<0) association with this value
 */
data class ValueExtraction(
    val axis: ValueAxis,
    val confidence: Double,
    val sentiment: Double
) {
    init {
        require(confidence in 0.0..1.0) {
            "Confidence must be between 0.0 and 1.0"
        }
        require(sentiment in -1.0..1.0) {
            "Sentiment must be between -1.0 and 1.0"
        }
    }

    /**
     * Whether this extraction is confident enough to use.
     */
    val isSignificant: Boolean
        get() = confidence >= 0.5
}
