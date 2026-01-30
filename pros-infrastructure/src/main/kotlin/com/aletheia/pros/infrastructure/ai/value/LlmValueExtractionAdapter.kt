package com.aletheia.pros.infrastructure.ai.value

import com.aletheia.pros.application.port.output.ValueExtraction
import com.aletheia.pros.application.port.output.ValueExtractionPort
import com.aletheia.pros.domain.value.ValueAxis
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * LLM-based adapter for extracting value axes from text.
 *
 * Uses structured JSON output to identify which values are
 * mentioned or implied in the user's text.
 */
@Component
class LlmValueExtractionAdapter(
    private val chatClientBuilder: ChatClient.Builder,
    private val objectMapper: ObjectMapper
) : ValueExtractionPort {

    private val chatClient: ChatClient = chatClientBuilder.build()

    companion object {
        private val SYSTEM_PROMPT = """
            You are a value extraction assistant. Analyze the given text and identify which of the 8 value dimensions are mentioned or implied.

            The 8 value axes are:
            - GROWTH: Personal development, learning, self-improvement
            - STABILITY: Security, predictability, routine
            - FINANCIAL: Money, economic security, compensation
            - AUTONOMY: Independence, freedom, control over own life
            - RELATIONSHIP: Social connections, belonging, family, friends
            - ACHIEVEMENT: Success, accomplishment, recognition
            - HEALTH: Physical/mental wellbeing, energy, fitness
            - MEANING: Purpose, significance, contribution to others

            Respond ONLY with a JSON array. Each object should have:
            - axis: The value axis name (GROWTH, STABILITY, etc.)
            - confidence: How confident you are (0.0 to 1.0)
            - sentiment: Positive (>0) or negative (<0) association (-1.0 to 1.0)

            Only include values that are actually present in the text. Return empty array [] if no values detected.

            Example response:
            [{"axis":"GROWTH","confidence":0.8,"sentiment":0.6},{"axis":"FINANCIAL","confidence":0.6,"sentiment":-0.3}]
        """.trimIndent()
    }

    override suspend fun extractValues(text: String): List<ValueExtraction> {
        logger.debug { "Extracting values from text: ${text.take(50)}..." }

        return try {
            val prompt = Prompt(
                listOf(
                    SystemMessage(SYSTEM_PROMPT),
                    UserMessage("Analyze this text and extract values:\n\n$text")
                )
            )

            val response = chatClient.prompt(prompt).call().content()

            if (response.isNullOrBlank()) {
                logger.debug { "Empty response from LLM" }
                return emptyList()
            }

            parseResponse(response)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to extract values, returning empty list" }
            emptyList()
        }
    }

    private fun parseResponse(response: String): List<ValueExtraction> {
        return try {
            // Extract JSON array from response (handle potential markdown formatting)
            val jsonContent = extractJsonArray(response)
            val rawExtractions: List<RawExtraction> = objectMapper.readValue(jsonContent)

            rawExtractions.mapNotNull { raw ->
                try {
                    val axis = ValueAxis.valueOf(raw.axis.uppercase())
                    ValueExtraction(
                        axis = axis,
                        confidence = raw.confidence.coerceIn(0.0, 1.0),
                        sentiment = raw.sentiment.coerceIn(-1.0, 1.0)
                    )
                } catch (e: IllegalArgumentException) {
                    logger.debug { "Unknown axis: ${raw.axis}" }
                    null
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse value extraction response: $response" }
            emptyList()
        }
    }

    private fun extractJsonArray(response: String): String {
        // Try to find JSON array in response
        val trimmed = response.trim()

        // If already a JSON array, return as-is
        if (trimmed.startsWith("[")) {
            return trimmed.substringAfter("[").substringBeforeLast("]").let { "[$it]" }
        }

        // Try to extract from markdown code block
        val codeBlockMatch = Regex("```(?:json)?\\s*\\n?(.+?)\\n?```", RegexOption.DOT_MATCHES_ALL)
            .find(trimmed)

        if (codeBlockMatch != null) {
            return codeBlockMatch.groupValues[1].trim()
        }

        // Return as-is and hope for the best
        return trimmed
    }

    private data class RawExtraction(
        val axis: String,
        val confidence: Double,
        val sentiment: Double
    )
}
