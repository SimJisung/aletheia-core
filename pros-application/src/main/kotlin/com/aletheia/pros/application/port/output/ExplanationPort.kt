package com.aletheia.pros.application.port.output

import com.aletheia.pros.domain.decision.Decision
import com.aletheia.pros.domain.fragment.ThoughtFragment

/**
 * Output port for LLM-based explanation generation.
 *
 * CRITICAL CONSTRAINTS:
 * - The LLM explains WHY results were computed
 * - The LLM does NOT recommend, suggest, or advise
 * - The LLM does NOT judge user's values or decisions
 * - Output must be neutral and descriptive only
 *
 * Implementing adapters: OpenAI, Anthropic, local LLM, etc.
 */
interface ExplanationPort {

    /**
     * Generates an explanation for a decision result.
     *
     * The explanation describes:
     * - Why the probabilities are what they are
     * - What evidence (fragments) influenced the result
     * - How value alignments were computed
     *
     * The explanation does NOT include:
     * - Recommendations ("you should choose A")
     * - Advice ("it would be better to...")
     * - Judgment ("A is the better option")
     *
     * @param decision The decision with computed results
     * @param evidenceFragments The fragments used as evidence
     * @return The generated explanation
     */
    suspend fun explainDecision(
        decision: Decision,
        evidenceFragments: List<ThoughtFragment>
    ): ExplanationResult

    /**
     * Summarizes a collection of fragments.
     *
     * The summary is descriptive only, no judgment or advice.
     *
     * @param fragments The fragments to summarize
     * @return A neutral summary
     */
    suspend fun summarizeFragments(fragments: List<ThoughtFragment>): String
}

/**
 * Result of explanation generation.
 */
data class ExplanationResult(
    /**
     * Main explanation summary.
     * Describes WHY results are what they are.
     */
    val summary: String,

    /**
     * Summary of the evidence used.
     * Neutral description of fragments.
     */
    val evidenceSummary: String,

    /**
     * Summary of value alignment.
     * Describes how options relate to user's values.
     */
    val valueSummary: String
)
