package com.aletheia.pros.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * JPA Entity for DecisionFeedback.
 *
 * Maps to the decision_feedbacks table.
 * Stores user feedback on decisions (SATISFIED, NEUTRAL, REGRET).
 */
@Entity
@Table(
    name = "decision_feedbacks",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["decision_id"])
    ]
)
class DecisionFeedbackEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decision_id", nullable = false, updatable = false)
    val decision: DecisionEntity,

    @Column(name = "feedback_type", nullable = false, updatable = false, length = 20)
    val feedbackType: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecisionFeedbackEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "DecisionFeedbackEntity(id=$id, feedbackType=$feedbackType)"
}
