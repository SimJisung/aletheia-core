package com.aletheia.pros.application.usecase.decision

import com.aletheia.pros.application.port.input.DecisionExplanation
import com.aletheia.pros.application.port.output.ExplanationPort
import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.decision.DecisionRepository
import com.aletheia.pros.domain.fragment.FragmentRepository

/**
 * Use case for generating LLM-based explanations for decisions.
 *
 * This use case retrieves a decision and its evidence fragments,
 * then delegates to the ExplanationPort to generate a human-readable explanation.
 *
 * CONSTRAINTS:
 * - The explanation describes WHY results were computed
 * - The explanation does NOT recommend or advise
 * - The explanation summarizes evidence without judgment
 */
class GetDecisionExplanationUseCase(
    private val decisionRepository: DecisionRepository,
    private val fragmentRepository: FragmentRepository,
    private val explanationPort: ExplanationPort
) {
    /**
     * Generates an explanation for the specified decision.
     *
     * @param decisionId The ID of the decision to explain
     * @return The generated explanation, or null if the decision is not found
     * @throws DecisionNotFoundException if the decision does not exist
     */
    suspend fun execute(decisionId: DecisionId): DecisionExplanation {
        // 1. Fetch the decision
        val decision = decisionRepository.findById(decisionId)
            ?: throw DecisionNotFoundException(decisionId)

        // 2. Fetch evidence fragments
        val evidenceFragmentIds = decision.result.evidenceFragmentIds
        val evidenceFragments = if (evidenceFragmentIds.isNotEmpty()) {
            fragmentRepository.findByIds(evidenceFragmentIds)
        } else {
            emptyList()
        }

        // 3. Generate explanation using LLM
        val explanationResult = explanationPort.explainDecision(decision, evidenceFragments)

        // 4. Map to DecisionExplanation
        return DecisionExplanation(
            decisionId = decisionId,
            summary = explanationResult.summary,
            evidenceSummary = explanationResult.evidenceSummary,
            valueSummary = explanationResult.valueSummary
        )
    }
}

/**
 * Exception thrown when a decision is not found.
 */
class DecisionNotFoundException(decisionId: DecisionId) : RuntimeException(
    "Decision not found: ${decisionId.value}"
)
