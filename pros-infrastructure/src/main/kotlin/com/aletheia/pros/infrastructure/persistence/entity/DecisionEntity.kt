package com.aletheia.pros.infrastructure.persistence.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * JPA Entity for Decision.
 *
 * Maps to the decisions table.
 * Stores decision projections with computed probabilities and regret risks.
 */
@Entity
@Table(name = "decisions")
class DecisionEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,

    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,

    @Column(name = "title", nullable = false, updatable = false, length = 500)
    val title: String,

    @Column(name = "option_a", nullable = false, updatable = false, columnDefinition = "TEXT")
    val optionA: String,

    @Column(name = "option_b", nullable = false, updatable = false, columnDefinition = "TEXT")
    val optionB: String,

    @Column(name = "priority_axis", length = 50)
    val priorityAxis: String? = null,

    @Column(name = "probability_a", nullable = false, updatable = false)
    val probabilityA: Double,

    @Column(name = "probability_b", nullable = false, updatable = false)
    val probabilityB: Double,

    @Column(name = "regret_risk_a", nullable = false, updatable = false)
    val regretRiskA: Double,

    @Column(name = "regret_risk_b", nullable = false, updatable = false)
    val regretRiskB: Double,

    @Column(name = "evidence_fragment_ids", nullable = false, columnDefinition = "UUID[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    val evidenceFragmentIds: Array<UUID> = emptyArray(),

    @Column(name = "value_alignment", nullable = false, columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    val valueAlignment: Map<String, Double> = emptyMap(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    // Explanation fields (nullable, populated on first request)
    @Column(name = "explanation_summary", columnDefinition = "TEXT")
    var explanationSummary: String? = null,

    @Column(name = "explanation_evidence_summary", columnDefinition = "TEXT")
    var explanationEvidenceSummary: String? = null,

    @Column(name = "explanation_value_summary", columnDefinition = "TEXT")
    var explanationValueSummary: String? = null,

    @Column(name = "explanation_generated_at")
    var explanationGeneratedAt: Instant? = null
) {
    @OneToOne(mappedBy = "decision", fetch = FetchType.LAZY)
    var feedback: DecisionFeedbackEntity? = null

    /**
     * Updates the explanation fields.
     */
    fun updateExplanation(
        summary: String,
        evidenceSummary: String,
        valueSummary: String,
        generatedAt: Instant
    ) {
        this.explanationSummary = summary
        this.explanationEvidenceSummary = evidenceSummary
        this.explanationValueSummary = valueSummary
        this.explanationGeneratedAt = generatedAt
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecisionEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "DecisionEntity(id=$id, title=$title)"
}
