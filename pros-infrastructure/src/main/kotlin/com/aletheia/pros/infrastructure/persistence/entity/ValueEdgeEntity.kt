package com.aletheia.pros.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * JPA Entity for ValueEdge.
 *
 * Maps to the value_edges table.
 * IMPORTANT: CONFLICT edges are never deleted - contradictions are preserved.
 */
@Entity
@Table(
    name = "value_edges",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "from_axis", "to_axis"])
    ]
)
class ValueEdgeEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,

    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,

    @Column(name = "from_axis", nullable = false, updatable = false, length = 50)
    val fromAxis: String,

    @Column(name = "to_axis", nullable = false, updatable = false, length = 50)
    val toAxis: String,

    @Column(name = "edge_type", nullable = false, updatable = false, length = 20)
    val edgeType: String,

    @Column(name = "weight", nullable = false)
    var weight: Double = 0.0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ValueEdgeEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "ValueEdgeEntity(id=$id, from=$fromAxis, to=$toAxis, type=$edgeType)"
}
