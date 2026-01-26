package com.aletheia.pros.application.port.input

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.value.ValueAxis
import com.aletheia.pros.domain.value.ValueEdge
import com.aletheia.pros.domain.value.ValueGraph
import com.aletheia.pros.domain.value.ValueNode

/**
 * Input port for Value Graph operations.
 *
 * The Value Graph represents the user's value structure derived from
 * their thought fragments. It shows:
 * - How the user feels about each of the 8 value axes
 * - Relationships (support/conflict) between values
 *
 * IMPORTANT DESIGN PRINCIPLES:
 * - Contradictions are preserved, not resolved
 * - This is descriptive (what IS), not prescriptive (what SHOULD BE)
 * - No judgment on whether values are "good" or "bad"
 */
interface ValueGraphInputPort {

    /**
     * Gets the complete value graph for a user.
     *
     * Returns all 8 value nodes and their edges.
     */
    suspend fun getValueGraph(userId: UserId): ValueGraph?

    /**
     * Gets a specific value node.
     */
    suspend fun getValueNode(userId: UserId, axis: ValueAxis): ValueNode?

    /**
     * Gets all edges for a user's value graph.
     */
    suspend fun getValueEdges(userId: UserId): List<ValueEdge>

    /**
     * Gets conflicts in the user's value graph.
     *
     * Note: Conflicts are normal and expected. This is informational,
     * not a problem to be fixed.
     */
    suspend fun getValueConflicts(userId: UserId): List<ValueConflict>

    /**
     * Gets a summary of the user's value profile.
     */
    suspend fun getValueSummary(userId: UserId): ValueSummary
}

/**
 * Represents a conflict between two values.
 *
 * Conflicts are normal - humans naturally have competing values.
 * This is informational, not judgmental.
 */
data class ValueConflict(
    val axis1: ValueAxis,
    val axis2: ValueAxis,
    val strength: Double,
    val description: String
)

/**
 * Summary of a user's value profile.
 */
data class ValueSummary(
    val userId: UserId,
    val topPositiveValues: List<ValueAxis>,
    val topNegativeValues: List<ValueAxis>,
    val dominantTrend: String,
    val conflictCount: Int,
    val totalFragments: Int
)
