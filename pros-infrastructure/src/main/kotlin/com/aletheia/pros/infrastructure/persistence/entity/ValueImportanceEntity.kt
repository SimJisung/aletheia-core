package com.aletheia.pros.infrastructure.persistence.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * JPA Entity for ValueImportance.
 *
 * Maps to the value_importance table.
 * Stores user's explicit importance ratings for value axes.
 */
@Entity
@Table(
    name = "value_importance",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id"])
    ]
)
class ValueImportanceEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,

    @Column(name = "user_id", nullable = false, unique = true)
    val userId: UUID,

    @Column(name = "importance_map", columnDefinition = "JSONB", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    val importanceMap: Map<String, Double>,

    @Column(name = "version", nullable = false)
    val version: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ValueImportanceEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "ValueImportanceEntity(id=$id, userId=$userId, version=$version)"
}
