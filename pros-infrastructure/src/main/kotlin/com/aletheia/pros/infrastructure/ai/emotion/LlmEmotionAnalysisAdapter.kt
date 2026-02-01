package com.aletheia.pros.infrastructure.ai.emotion

import com.aletheia.pros.application.exception.QuotaExceededException
import com.aletheia.pros.application.port.output.EmotionAnalysisPort
import com.aletheia.pros.application.port.output.EmotionAnalysisResult
import com.aletheia.pros.domain.fragment.Arousal
import com.aletheia.pros.domain.fragment.MoodValence
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.retry.NonTransientAiException
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Adapter implementing EmotionAnalysisPort using LLM.
 *
 * Uses structured output to extract emotion scores from text.
 * This is NOT a judgment of the user's emotions - it's a signal extraction
 * to help with later retrieval and analysis.
 */
@Component
class LlmEmotionAnalysisAdapter(
    private val chatClientBuilder: ChatClient.Builder,
    private val objectMapper: ObjectMapper
) : EmotionAnalysisPort {

    private val chatClient: ChatClient = chatClientBuilder.build()

    companion object {
        private val SYSTEM_PROMPT = """
            You are an emotion analysis assistant. Your task is to analyze the emotional content of text.

            IMPORTANT: You are NOT judging the user. You are extracting emotional signals from text.

            Analyze the text and return a JSON object with:
            - valence: A number from -1.0 (very negative) to 1.0 (very positive)
            - arousal: A number from 0.0 (calm/low energy) to 1.0 (excited/high energy)
            - confidence: Your confidence in this analysis from 0.0 to 1.0

            Examples:
            - "I got promoted today!" -> {"valence": 0.8, "arousal": 0.7, "confidence": 0.9}
            - "Feeling tired and unmotivated" -> {"valence": -0.4, "arousal": 0.2, "confidence": 0.8}
            - "Had lunch" -> {"valence": 0.1, "arousal": 0.3, "confidence": 0.7}

            Return ONLY the JSON object, no other text.
        """.trimIndent()
    }

    override suspend fun analyzeEmotion(text: String): EmotionAnalysisResult {
        logger.debug { "Analyzing emotion for text (length=${text.length})" }

        return try {
            val prompt = Prompt(
                listOf(
                    SystemMessage(SYSTEM_PROMPT),
                    UserMessage(text)
                )
            )

            val response = chatClient.prompt(prompt).call().content()

            parseResponse(response ?: "{}")
        } catch (e: NonTransientAiException) {
            logger.error(e) { "OpenAI API quota exceeded or non-retryable error in emotion analysis" }
            // Check if it's a quota error by examining the error message
            if (e.message?.contains("quota", ignoreCase = true) == true ||
                e.message?.contains("insufficient_quota", ignoreCase = true) == true) {
                throw QuotaExceededException("OpenAI API quota exceeded", e)
            }
            // For other non-transient errors, propagate the exception
            throw e
        } catch (e: Exception) {
            // For transient errors (network timeouts, etc.), use defaults
            logger.warn(e) { "Failed to analyze emotion, using defaults" }
            // Return neutral defaults on failure
            EmotionAnalysisResult(
                valence = MoodValence.NEUTRAL,
                arousal = Arousal.MODERATE,
                confidence = 0.0
            )
        }
    }

    private fun parseResponse(response: String): EmotionAnalysisResult {
        return try {
            val json = objectMapper.readTree(response.trim())

            val valence = json.get("valence")?.asDouble() ?: 0.0
            val arousal = json.get("arousal")?.asDouble() ?: 0.5
            val confidence = json.get("confidence")?.asDouble() ?: 0.5

            EmotionAnalysisResult(
                valence = MoodValence(valence.coerceIn(-1.0, 1.0)),
                arousal = Arousal(arousal.coerceIn(0.0, 1.0)),
                confidence = confidence.coerceIn(0.0, 1.0)
            )
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse emotion response: $response" }
            EmotionAnalysisResult(
                valence = MoodValence.NEUTRAL,
                arousal = Arousal.MODERATE,
                confidence = 0.0
            )
        }
    }
}
