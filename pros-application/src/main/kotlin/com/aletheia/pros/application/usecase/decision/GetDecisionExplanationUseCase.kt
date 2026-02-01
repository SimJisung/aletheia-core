package com.aletheia.pros.application.usecase.decision

import com.aletheia.pros.application.port.input.DecisionExplanation
import com.aletheia.pros.application.port.output.ExplanationPort
import com.aletheia.pros.domain.common.DecisionId
import com.aletheia.pros.domain.decision.DecisionRepository
import com.aletheia.pros.domain.fragment.FragmentRepository
import com.aletheia.pros.domain.decision.DecisionExplanation as DomainExplanation

/**
 * Use case for generating LLM-based explanations for decisions.
 *
 * This use case retrieves a decision and its evidence fragments,
 * then delegates to the ExplanationPort to generate a human-readable explanation.
 *
 * IMPORTANT: Explanations are cached in the database after first generation
 * to avoid repeated LLM calls and reduce latency on subsequent requests.
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
     * Gets or generates an explanation for the specified decision.
     *
     * If the decision already has a cached explanation, it is returned immediately.
     * Otherwise, a new explanation is generated via LLM and saved for future requests.
     *
     * @param decisionId The ID of the decision to explain
     * @return The explanation (cached or newly generated)
     * @throws DecisionNotFoundException if the decision does not exist
     */
    suspend fun execute(decisionId: DecisionId): DecisionExplanation {
        // 1. Fetch the decision
        val decision = decisionRepository.findById(decisionId)
            ?: throw DecisionNotFoundException(decisionId)

        // 2. Check if explanation is already cached
        if (decision.hasExplanation) {
            val cached = decision.explanation!!
            return DecisionExplanation(
                decisionId = decisionId,
                summary = cached.summary,
                evidenceSummary = cached.evidenceSummary,
                valueSummary = cached.valueSummary
            )
        }

        // 3. Fetch evidence fragments for LLM context
        val evidenceFragmentIds = decision.result.evidenceFragmentIds
        val evidenceFragments = if (evidenceFragmentIds.isNotEmpty()) {
            fragmentRepository.findByIds(evidenceFragmentIds)
        } else {
            emptyList()
        }

        // 4. Generate explanation using LLM
        val explanationResult = explanationPort.explainDecision(decision, evidenceFragments)

        // 5. Save explanation to database for future requests
        val domainExplanation = DomainExplanation.create(
            summary = explanationResult.summary,
            evidenceSummary = explanationResult.evidenceSummary,
            valueSummary = explanationResult.valueSummary
        )
        decisionRepository.updateExplanation(decisionId, domainExplanation)

        // 6. Return the explanation
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
