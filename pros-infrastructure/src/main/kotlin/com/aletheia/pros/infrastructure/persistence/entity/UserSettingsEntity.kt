package com.aletheia.pros.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * JPA Entity for UserSettings.
 *
 * Maps to the user_settings table.
 * Stores user-specific learning parameters (lambda, regret prior).
 */
@Entity
@Table(
    name = "user_settings",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["user_id"])
    ]
)
class UserSettingsEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,

    @Column(name = "user_id", nullable = false, unique = true)
    val userId: UUID,

    @Column(name = "lambda", nullable = false)
    var lambda: Double = 1.0,

    @Column(name = "regret_prior", nullable = false)
    var regretPrior: Double = 0.2,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserSettingsEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "UserSettingsEntity(id=$id, userId=$userId)"
}
