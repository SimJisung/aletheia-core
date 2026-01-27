package com.aletheia.pros.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * JPA Entity for ValueNode.
 *
 * Maps to the value_nodes table.
 * Each user has exactly 8 nodes (one per ValueAxis).
 */
@Entity
@Table(
    name = "value_nodes",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id", "axis"])
    ]
)
class ValueNodeEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,

    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,

    @Column(name = "axis", nullable = false, updatable = false, length = 50)
    val axis: String,

    @Column(name = "avg_valence", nullable = false)
    var avgValence: Double = 0.0,

    @Column(name = "recent_trend", nullable = false, length = 20)
    var recentTrend: String = "NEUTRAL",

    @Column(name = "fragment_count", nullable = false)
    var fragmentCount: Double = 0.0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ValueNodeEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "ValueNodeEntity(id=$id, axis=$axis)"
}
