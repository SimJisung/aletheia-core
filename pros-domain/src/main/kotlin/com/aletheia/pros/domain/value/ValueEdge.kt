package com.aletheia.pros.domain.value

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueEdgeId
import java.time.Instant

/**
 * ValueEdge represents a relationship between two value axes.
 *
 * Edges can be:
 * - SUPPORT: The two values tend to be satisfied together
 * - CONFLICT: The two values tend to be in tension
 *
 * IMPORTANT DESIGN PRINCIPLE:
 * Conflict edges are NEVER automatically removed. Contradictions in values
 * are normal and expected. The system preserves these tensions rather than
 * trying to resolve them.
 */
data class ValueEdge(
    val id: ValueEdgeId,
    val userId: UserId,
    val fromAxis: ValueAxis,
    val toAxis: ValueAxis,
    val edgeType: EdgeType,
    val weight: Double,
    val updatedAt: Instant
) {
    init {
        require(fromAxis != toAxis) {
            "Edge cannot connect a value to itself"
        }
        require(weight in 0.0..1.0) {
            "Weight must be between 0.0 and 1.0"
        }
    }

    /**
     * Whether this edge represents a conflict between values.
     */
    val isConflict: Boolean get() = edgeType == EdgeType.CONFLICT

    /**
     * Whether this edge represents mutual support between values.
     */
    val isSupport: Boolean get() = edgeType == EdgeType.SUPPORT

    /**
     * Whether this edge has significant weight.
     */
    val isSignificant: Boolean get() = weight >= SIGNIFICANCE_THRESHOLD

    /**
     * Updates the edge weight based on new evidence.
     *
     * Note: EdgeType is preserved. We do NOT flip CONFLICT to SUPPORT
     * or vice versa - that would be trying to "resolve" contradictions.
     */
    fun updateWeight(
        newWeight: Double,
        updatedAt: Instant = Instant.now()
    ): ValueEdge {
        require(newWeight in 0.0..1.0) { "Weight must be between 0.0 and 1.0" }
        return copy(
            weight = newWeight,
            updatedAt = updatedAt
        )
    }

    /**
     * Returns a normalized key for this edge (order-independent).
     */
    val normalizedKey: Pair<ValueAxis, ValueAxis>
        get() = if (fromAxis.ordinal < toAxis.ordinal) {
            fromAxis to toAxis
        } else {
            toAxis to fromAxis
        }

    companion object {
        const val SIGNIFICANCE_THRESHOLD = 0.3

        /**
         * Creates a new edge between two values.
         */
        fun create(
            userId: UserId,
            fromAxis: ValueAxis,
            toAxis: ValueAxis,
            edgeType: EdgeType,
            initialWeight: Double = 0.0,
            createdAt: Instant = Instant.now()
        ): ValueEdge = ValueEdge(
            id = ValueEdgeId.generate(),
            userId = userId,
            fromAxis = fromAxis,
            toAxis = toAxis,
            edgeType = edgeType,
            weight = initialWeight,
            updatedAt = createdAt
        )
    }
}

/**
 * EdgeType defines the nature of the relationship between two values.
 *
 * Both types are valid and preserved. The system does NOT try to
 * eliminate conflicts or contradictions.
 */
enum class EdgeType {
    /**
     * The two values tend to be satisfied together.
     * When one is fulfilled, the other often is too.
     */
    SUPPORT,

    /**
     * The two values tend to be in tension.
     * Pursuing one often comes at the cost of the other.
     *
     * IMPORTANT: Conflict edges are NEVER automatically removed.
     */
    CONFLICT
}
