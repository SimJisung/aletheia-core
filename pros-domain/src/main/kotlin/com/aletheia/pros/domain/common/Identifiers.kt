package com.aletheia.pros.domain.common

import java.util.UUID

/**
 * Type-safe identifier for ThoughtFragment entities.
 */
@JvmInline
value class FragmentId(val value: UUID) {
    companion object {
        fun generate(): FragmentId = FragmentId(UUID.randomUUID())
        fun from(value: String): FragmentId = FragmentId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}

/**
 * Type-safe identifier for User entities.
 */
@JvmInline
value class UserId(val value: UUID) {
    companion object {
        fun generate(): UserId = UserId(UUID.randomUUID())
        fun from(value: String): UserId = UserId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}

/**
 * Type-safe identifier for Decision entities.
 */
@JvmInline
value class DecisionId(val value: UUID) {
    companion object {
        fun generate(): DecisionId = DecisionId(UUID.randomUUID())
        fun from(value: String): DecisionId = DecisionId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}

/**
 * Type-safe identifier for ValueNode entities.
 */
@JvmInline
value class ValueNodeId(val value: UUID) {
    companion object {
        fun generate(): ValueNodeId = ValueNodeId(UUID.randomUUID())
        fun from(value: String): ValueNodeId = ValueNodeId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}

/**
 * Type-safe identifier for ValueEdge entities.
 */
@JvmInline
value class ValueEdgeId(val value: UUID) {
    companion object {
        fun generate(): ValueEdgeId = ValueEdgeId(UUID.randomUUID())
        fun from(value: String): ValueEdgeId = ValueEdgeId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}

/**
 * Type-safe identifier for Feedback entities.
 */
@JvmInline
value class FeedbackId(val value: UUID) {
    companion object {
        fun generate(): FeedbackId = FeedbackId(UUID.randomUUID())
        fun from(value: String): FeedbackId = FeedbackId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}
