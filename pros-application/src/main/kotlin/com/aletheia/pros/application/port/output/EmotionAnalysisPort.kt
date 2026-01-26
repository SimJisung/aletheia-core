package com.aletheia.pros.application.port.output

import com.aletheia.pros.domain.fragment.Arousal
import com.aletheia.pros.domain.fragment.MoodValence

/**
 * Output port for emotion analysis.
 *
 * This is a secondary port (driven adapter interface) that defines
 * how the application gets emotion analysis capabilities.
 *
 * Implementing adapters: Simple rule-based, ML model, LLM-based, etc.
 */
interface EmotionAnalysisPort {

    /**
     * Analyzes the emotional content of text.
     *
     * @param text The text to analyze
     * @return The emotional analysis result
     */
    suspend fun analyzeEmotion(text: String): EmotionAnalysisResult
}

/**
 * Result of emotion analysis.
 */
data class EmotionAnalysisResult(
    val valence: MoodValence,
    val arousal: Arousal,
    val confidence: Double
) {
    init {
        require(confidence in 0.0..1.0) {
            "Confidence must be between 0.0 and 1.0"
        }
    }
}
