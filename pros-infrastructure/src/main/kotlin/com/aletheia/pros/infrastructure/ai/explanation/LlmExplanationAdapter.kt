package com.aletheia.pros.infrastructure.ai.explanation

import com.aletheia.pros.application.port.output.ExplanationPort
import com.aletheia.pros.application.port.output.ExplanationResult
import com.aletheia.pros.domain.decision.Decision
import com.aletheia.pros.domain.fragment.ThoughtFragment
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Adapter implementing ExplanationPort using LLM.
 *
 * CRITICAL: This adapter enforces strict guardrails to prevent the LLM from:
 * - Recommending or suggesting choices
 * - Judging user's values or decisions
 * - Using prescriptive language
 *
 * The LLM ONLY explains WHY results were computed, not what the user should do.
 */
@Component
class LlmExplanationAdapter(
    private val chatClientBuilder: ChatClient.Builder,
    @Value("\${pros.explanation.system-prompt:}")
    private val configuredSystemPrompt: String
) : ExplanationPort {

    private val chatClient: ChatClient = chatClientBuilder.build()

    companion object {
        private val DEFAULT_SYSTEM_PROMPT = """
            You are an explanation assistant for PROS (Personal Reasoning OS).

            STRICT RULES:
            1. NEVER recommend, suggest, or advise any choice
            2. NEVER use phrases like "you should", "I recommend", "better option"
            3. NEVER judge user's values or emotions
            4. ONLY explain WHY the calculation produced these results
            5. ONLY summarize the evidence fragments
            6. ALWAYS use neutral, descriptive language

            Your role is to TRANSLATE calculations into human-readable explanations.
            You do NOT make decisions. The user makes decisions.

            Use Korean language for the explanation.
        """.trimIndent()
    }

    private val systemPrompt: String
        get() = configuredSystemPrompt.ifBlank { DEFAULT_SYSTEM_PROMPT }

    override suspend fun explainDecision(
        decision: Decision,
        evidenceFragments: List<ThoughtFragment>
    ): ExplanationResult {
        logger.debug { "Generating explanation for decision: ${decision.id}" }

        val userPrompt = buildDecisionPrompt(decision, evidenceFragments)

        return try {
            val prompt = Prompt(
                listOf(
                    SystemMessage(systemPrompt),
                    UserMessage(userPrompt)
                )
            )

            val response = chatClient.prompt(prompt).call().content() ?: ""

            parseExplanationResponse(response)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to generate explanation, using default" }
            defaultExplanation(decision)
        }
    }

    override suspend fun summarizeFragments(fragments: List<ThoughtFragment>): String {
        if (fragments.isEmpty()) return "기록된 생각 파편이 없습니다."

        logger.debug { "Summarizing ${fragments.size} fragments" }

        val fragmentTexts = fragments.take(10).mapIndexed { index, fragment ->
            "${index + 1}. \"${fragment.textRaw.take(100)}...\""
        }.joinToString("\n")

        val userPrompt = """
            다음 생각 파편들을 중립적으로 요약해주세요. 판단하거나 조언하지 마세요.

            파편들:
            $fragmentTexts

            요약 (판단 없이, 사실만):
        """.trimIndent()

        return try {
            val prompt = Prompt(
                listOf(
                    SystemMessage(systemPrompt),
                    UserMessage(userPrompt)
                )
            )

            chatClient.prompt(prompt).call().content() ?: "요약을 생성할 수 없습니다."
        } catch (e: Exception) {
            logger.warn(e) { "Failed to summarize fragments" }
            "요약을 생성할 수 없습니다."
        }
    }

    private fun buildDecisionPrompt(
        decision: Decision,
        evidenceFragments: List<ThoughtFragment>
    ): String {
        val fragmentSummary = evidenceFragments.take(5).mapIndexed { index, fragment ->
            "  ${index + 1}. \"${fragment.textRaw.take(100)}...\" (감정: ${formatValence(fragment.moodValence.value)})"
        }.joinToString("\n")

        return """
            다음 결정 투영 결과를 설명해주세요. 추천하거나 조언하지 마세요.

            결정: ${decision.title}
            선택지 A: ${decision.optionA}
            선택지 B: ${decision.optionB}

            계산 결과:
            - A 적합 확률: ${decision.result.probabilityA.percentage}%
            - B 적합 확률: ${decision.result.probabilityB.percentage}%
            - A 후회 위험: ${decision.result.regretRiskA.percentage}%
            - B 후회 위험: ${decision.result.regretRiskB.percentage}%

            근거 파편 (과거 기록):
            $fragmentSummary

            다음 형식으로 응답해주세요:
            [요약]
            (왜 이런 결과가 나왔는지 2-3문장으로 설명)

            [근거]
            (근거 파편들의 공통점 1-2문장)

            [가치]
            (이 결정과 관련된 가치에 대해 1-2문장)
        """.trimIndent()
    }

    private fun formatValence(valence: Double): String {
        return when {
            valence >= 0.5 -> "긍정적"
            valence >= 0.1 -> "약간 긍정적"
            valence >= -0.1 -> "중립"
            valence >= -0.5 -> "약간 부정적"
            else -> "부정적"
        }
    }

    private fun parseExplanationResponse(response: String): ExplanationResult {
        // Simple parsing - extract sections based on markers
        val summaryRegex = """\[요약\]\s*(.+?)(?=\[|$)""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val evidenceRegex = """\[근거\]\s*(.+?)(?=\[|$)""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val valueRegex = """\[가치\]\s*(.+?)(?=\[|$)""".toRegex(RegexOption.DOT_MATCHES_ALL)

        val summary = summaryRegex.find(response)?.groupValues?.get(1)?.trim()
            ?: response.take(200)
        val evidenceSummary = evidenceRegex.find(response)?.groupValues?.get(1)?.trim()
            ?: "근거 파편을 기반으로 계산되었습니다."
        val valueSummary = valueRegex.find(response)?.groupValues?.get(1)?.trim()
            ?: "가치 정보를 분석할 수 없습니다."

        return ExplanationResult(
            summary = summary,
            evidenceSummary = evidenceSummary,
            valueSummary = valueSummary
        )
    }

    private fun defaultExplanation(decision: Decision): ExplanationResult {
        return ExplanationResult(
            summary = "이 결과는 과거 ${decision.result.evidenceFragmentIds.size}개의 생각 파편을 기반으로 계산되었습니다. " +
                    "A 선택지의 적합 확률은 ${decision.result.probabilityA.percentage}%, " +
                    "B 선택지는 ${decision.result.probabilityB.percentage}%입니다.",
            evidenceSummary = "과거 기록된 생각 파편들과의 유사도를 기반으로 분석되었습니다.",
            valueSummary = "가치 정렬 정보는 분석 중입니다."
        )
    }
}
