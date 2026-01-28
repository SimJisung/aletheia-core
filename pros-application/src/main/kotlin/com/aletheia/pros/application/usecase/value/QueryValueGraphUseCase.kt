package com.aletheia.pros.application.usecase.value

import com.aletheia.pros.application.port.input.ValueConflict
import com.aletheia.pros.application.port.input.ValueSummary
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.fragment.FragmentRepository
import com.aletheia.pros.domain.value.Trend
import com.aletheia.pros.domain.value.ValueAxis
import com.aletheia.pros.domain.value.ValueEdge
import com.aletheia.pros.domain.value.ValueGraph
import com.aletheia.pros.domain.value.ValueGraphRepository
import com.aletheia.pros.domain.value.ValueNode

/**
 * Use case for querying value graph information.
 *
 * This provides extended query capabilities for the value graph:
 * - Get all edges between value axes
 * - Get conflicts (tension between values)
 * - Get user's value profile summary
 *
 * IMPORTANT DESIGN PRINCIPLES:
 * - Conflicts are normal and expected
 * - This is descriptive, not prescriptive
 * - No judgment on "good" or "bad" values
 */
class QueryValueGraphUseCase(
    private val valueGraphRepository: ValueGraphRepository,
    private val fragmentRepository: FragmentRepository
) {

    /**
     * Gets the complete value graph for a user.
     */
    suspend fun getGraph(userId: UserId): ValueGraph? {
        return valueGraphRepository.findValueGraph(userId)
    }

    /**
     * Gets a specific value node.
     */
    suspend fun getNode(userId: UserId, axis: ValueAxis): ValueNode? {
        return valueGraphRepository.findNodeByUserAndAxis(userId, axis)
    }

    /**
     * Gets all edges in the user's value graph.
     */
    suspend fun getEdges(userId: UserId): List<ValueEdge> {
        return valueGraphRepository.findEdgesByUserId(userId)
    }

    /**
     * Gets value conflicts (tension between values).
     *
     * Note: Conflicts are NORMAL and expected. This is informational,
     * not a problem to be fixed.
     *
     * @param userId The user ID
     * @return List of conflicts with strength and description
     */
    suspend fun getConflicts(userId: UserId): List<ValueConflict> {
        val conflictEdges = valueGraphRepository.findConflictEdges(userId)

        return conflictEdges
            .filter { it.isSignificant }
            .map { edge ->
                ValueConflict(
                    axis1 = edge.fromAxis,
                    axis2 = edge.toAxis,
                    strength = edge.weight,
                    description = buildConflictDescription(edge)
                )
            }
            .sortedByDescending { it.strength }
    }

    /**
     * Gets a summary of the user's value profile.
     */
    suspend fun getSummary(userId: UserId): ValueSummary {
        val graph = valueGraphRepository.findValueGraph(userId)
            ?: return emptyValueSummary(userId)

        val fragmentCount = fragmentRepository.countByUserId(userId)

        val topPositive = graph.topPositiveValues(3).map { it.axis }
        val topNegative = graph.topNegativeValues(3).map { it.axis }
        val dominantTrend = calculateDominantTrend(graph.nodes)
        val significantConflicts = graph.conflicts.count { it.isSignificant }

        return ValueSummary(
            userId = userId,
            topPositiveValues = topPositive,
            topNegativeValues = topNegative,
            dominantTrend = dominantTrend,
            conflictCount = significantConflicts,
            totalFragments = fragmentCount.toInt()
        )
    }

    /**
     * Builds a neutral, descriptive text for a conflict.
     */
    private fun buildConflictDescription(edge: ValueEdge): String {
        val axis1Name = edge.fromAxis.displayNameEn
        val axis2Name = edge.toAxis.displayNameEn

        return when {
            edge.weight >= 0.7 -> "$axis1Name and $axis2Name show strong tension in your recorded thoughts."
            edge.weight >= 0.5 -> "$axis1Name and $axis2Name appear to be in moderate tension."
            else -> "$axis1Name and $axis2Name show some tension."
        }
    }

    /**
     * Calculates the dominant trend across all value nodes.
     */
    private fun calculateDominantTrend(nodes: List<ValueNode>): String {
        if (nodes.isEmpty()) return "NEUTRAL"

        val rising = nodes.count { it.recentTrend == Trend.RISING }
        val falling = nodes.count { it.recentTrend == Trend.FALLING }

        return when {
            rising > falling -> "RISING"
            falling > rising -> "FALLING"
            else -> "STABLE"
        }
    }

    /**
     * Creates an empty value summary for users with no data.
     */
    private fun emptyValueSummary(userId: UserId): ValueSummary {
        return ValueSummary(
            userId = userId,
            topPositiveValues = emptyList(),
            topNegativeValues = emptyList(),
            dominantTrend = "NEUTRAL",
            conflictCount = 0,
            totalFragments = 0
        )
    }
}
