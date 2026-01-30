package com.aletheia.pros.infrastructure.persistence.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * JPA Entity for User.
 */
@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,

    @Column(name = "email", nullable = false, unique = true)
    val email: String,

    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "last_login_at")
    val lastLoginAt: Instant? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true
)
