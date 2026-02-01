package com.aletheia.pros.domain.value

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueImportanceId
import java.time.Instant

/**
 * ValueImportance represents a user's explicit importance ratings for value axes.
 *
 * This is separate from ValueNode (which tracks implicit valence from fragments).
 * Users can explicitly set how important each value axis is to them on a 1-10 scale.
 *
 * Design Decision:
 * - Stored as a Map to allow partial updates (not all 8 axes required)
 * - Normalized to 0.0-1.0 internally for calculation consistency
 * - Immutable with version history for audit trail
 */
data class ValueImportance(
    val id: ValueImportanceId,
    val userId: UserId,
    val importanceMap: Map<ValueAxis, Double>,  // Normalized 0.0-1.0
    val version: Int,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        importanceMap.forEach { (axis, value) ->
            require(value in 0.0..1.0) {
                "Importance for $axis must be between 0.0 and 1.0, got: $value"
            }
        }
        require(version >= 1) { "Version must be at least 1" }
    }

    /**
     * Gets the importance for a specific axis.
     * Returns default value if not explicitly set.
     */
    fun getImportance(axis: ValueAxis): Double {
        return importanceMap[axis] ?: DEFAULT_IMPORTANCE
    }

    /**
     * Whether this axis has an explicit importance value.
     */
    fun hasExplicitImportance(axis: ValueAxis): Boolean {
        return importanceMap.containsKey(axis)
    }

    /**
     * Updates importance values and increments version.
     */
    fun update(
        newImportanceMap: Map<ValueAxis, Double>,
        updatedAt: Instant = Instant.now()
    ): ValueImportance {
        val mergedMap = importanceMap.toMutableMap().apply {
            putAll(newImportanceMap)
        }
        return copy(
            importanceMap = mergedMap,
            version = version + 1,
            updatedAt = updatedAt
        )
    }

    /**
     * Gets all importance values, including defaults for unset axes.
     */
    fun getAllImportances(): Map<ValueAxis, Double> {
        return ValueAxis.all().associateWith { axis ->
            importanceMap[axis] ?: DEFAULT_IMPORTANCE
        }
    }

    companion object {
        /**
         * Default importance when not explicitly set.
         * 0.5 represents neutral importance.
         */
        const val DEFAULT_IMPORTANCE = 0.5

        /**
         * Creates a new ValueImportance with initial values.
         */
        fun create(
            userId: UserId,
            importanceMap: Map<ValueAxis, Double>,
            createdAt: Instant = Instant.now()
        ): ValueImportance {
            // Normalize input (1-10 scale) to 0.0-1.0
            val normalizedMap = importanceMap.mapValues { (_, value) ->
                normalizeFromScale(value)
            }
            return ValueImportance(
                id = ValueImportanceId.generate(),
                userId = userId,
                importanceMap = normalizedMap,
                version = 1,
                createdAt = createdAt,
                updatedAt = createdAt
            )
        }

        /**
         * Creates with all axes set to default.
         */
        fun createDefault(
            userId: UserId,
            createdAt: Instant = Instant.now()
        ): ValueImportance = ValueImportance(
            id = ValueImportanceId.generate(),
            userId = userId,
            importanceMap = emptyMap(),
            version = 1,
            createdAt = createdAt,
            updatedAt = createdAt
        )

        /**
         * Converts 1-10 scale to 0.0-1.0 normalized value.
         */
        fun normalizeFromScale(value: Double, min: Double = 1.0, max: Double = 10.0): Double {
            return ((value - min) / (max - min)).coerceIn(0.0, 1.0)
        }

        /**
         * Converts 0.0-1.0 to 1-10 scale for display.
         */
        fun denormalizeToScale(normalized: Double, min: Double = 1.0, max: Double = 10.0): Double {
            return (normalized * (max - min) + min).coerceIn(min, max)
        }
    }
}
