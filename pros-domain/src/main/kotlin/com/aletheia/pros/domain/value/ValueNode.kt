package com.aletheia.pros.domain.value

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueNodeId
import java.time.Instant

/**
 * ValueNode represents the state of a single value axis for a user.
 *
 * Each user has exactly 8 ValueNodes (one per ValueAxis).
 * The node tracks:
 * - Average emotional valence toward this value
 * - Recent trend direction
 * - Number of associated fragments
 *
 * These statistics are updated incrementally as fragments are added.
 */
data class ValueNode(
    val id: ValueNodeId,
    val userId: UserId,
    val axis: ValueAxis,
    val avgValence: Double,
    val recentTrend: Trend,
    val fragmentCount: Int,
    val updatedAt: Instant
) {
    init {
        require(avgValence in -1.0..1.0) {
            "avgValence must be between -1.0 and 1.0"
        }
        require(fragmentCount >= 0) {
            "fragmentCount cannot be negative"
        }
    }

    /**
     * Whether this value has any associated fragments.
     */
    val hasFragments: Boolean get() = fragmentCount > 0

    /**
     * Whether the user feels positively about this value overall.
     */
    val isPositive: Boolean get() = avgValence > 0

    /**
     * Whether the user feels negatively about this value overall.
     */
    val isNegative: Boolean get() = avgValence < 0

    /**
     * Updates the node with a new fragment's contribution.
     *
     * @param fragmentValence The valence of the fragment toward this value
     * @param weight The soft-assignment weight (0.0 to 1.0)
     * @param newTrend The computed trend based on recent history
     */
    fun updateWithFragment(
        fragmentValence: Double,
        weight: Double,
        newTrend: Trend,
        updatedAt: Instant = Instant.now()
    ): ValueNode {
        require(fragmentValence in -1.0..1.0) { "fragmentValence must be between -1.0 and 1.0" }
        require(weight in 0.0..1.0) { "weight must be between 0.0 and 1.0" }

        // Weighted incremental average
        val newCount = fragmentCount + 1
        val newAvg = ((avgValence * fragmentCount) + (fragmentValence * weight)) / newCount

        return copy(
            avgValence = newAvg.coerceIn(-1.0, 1.0),
            recentTrend = newTrend,
            fragmentCount = newCount,
            updatedAt = updatedAt
        )
    }

    companion object {
        /**
         * Creates a new ValueNode with default (neutral) values.
         */
        fun createDefault(
            userId: UserId,
            axis: ValueAxis,
            createdAt: Instant = Instant.now()
        ): ValueNode = ValueNode(
            id = ValueNodeId.generate(),
            userId = userId,
            axis = axis,
            avgValence = 0.0,
            recentTrend = Trend.NEUTRAL,
            fragmentCount = 0,
            updatedAt = createdAt
        )

        /**
         * Creates all 8 value nodes for a new user.
         */
        fun createAllForUser(
            userId: UserId,
            createdAt: Instant = Instant.now()
        ): List<ValueNode> = ValueAxis.all().map { axis ->
            createDefault(userId, axis, createdAt)
        }
    }
}

/**
 * Trend represents the direction of change in a value's emotional association.
 */
enum class Trend {
    /**
     * The value is becoming more positively associated.
     */
    RISING,

    /**
     * The value is becoming more negatively associated.
     */
    FALLING,

    /**
     * No significant change in association.
     */
    NEUTRAL;

    companion object {
        /**
         * Computes trend from recent valence changes.
         *
         * @param recentValences List of recent valences (newest first)
         * @param threshold Minimum change to consider as a trend
         */
        fun compute(
            recentValences: List<Double>,
            threshold: Double = 0.1
        ): Trend {
            if (recentValences.size < 2) return NEUTRAL

            // Compare average of recent vs older
            val midpoint = recentValences.size / 2
            val recentAvg = recentValences.take(midpoint).average()
            val olderAvg = recentValences.drop(midpoint).average()

            val change = recentAvg - olderAvg

            return when {
                change > threshold -> RISING
                change < -threshold -> FALLING
                else -> NEUTRAL
            }
        }
    }
}
