package com.aletheia.pros.infrastructure.persistence.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * JPA Entity for ThoughtFragment.
 *
 * Maps to the thought_fragments table.
 * Note: This entity supports append-only semantics.
 */
@Entity
@Table(name = "thought_fragments")
class ThoughtFragmentEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,

    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,

    @Column(name = "text_raw", nullable = false, updatable = false, columnDefinition = "TEXT")
    val textRaw: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,

    @Column(name = "mood_valence", nullable = false, updatable = false)
    val moodValence: Double,

    @Column(name = "arousal", nullable = false, updatable = false)
    val arousal: Double,

    @Column(name = "topic_hint", updatable = false)
    val topicHint: String? = null,

    @Column(name = "embedding", columnDefinition = "vector(1536)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    var embedding: FloatArray? = null,

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
) {
    /**
     * Whether this fragment has been soft-deleted.
     */
    val isDeleted: Boolean get() = deletedAt != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThoughtFragmentEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "ThoughtFragmentEntity(id=$id, userId=$userId)"
}
