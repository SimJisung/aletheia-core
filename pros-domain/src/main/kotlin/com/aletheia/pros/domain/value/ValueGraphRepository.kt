package com.aletheia.pros.domain.value

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueEdgeId
import com.aletheia.pros.domain.common.ValueNodeId

/**
 * Repository interface for ValueGraph persistence.
 *
 * Implementation notes:
 * - Nodes should be created lazily when first fragment is associated
 * - Edges should NEVER be deleted (especially CONFLICT edges)
 * - Weight updates are the only allowed modification to edges
 */
interface ValueGraphRepository {

    // ==================== ValueNode Operations ====================

    /**
     * Saves or updates a value node.
     */
    fun saveNode(node: ValueNode): ValueNode

    /**
     * Finds a node by ID.
     */
    fun findNodeById(id: ValueNodeId): ValueNode?

    /**
     * Finds a node by user and axis.
     */
    fun findNodeByUserAndAxis(userId: UserId, axis: ValueAxis): ValueNode?

    /**
     * Finds all nodes for a user.
     * Returns exactly 8 nodes (one per axis) if the user has data.
     */
    fun findNodesByUserId(userId: UserId): List<ValueNode>

    /**
     * Initializes all 8 value nodes for a user if missing.
     * Must be safe for concurrent calls.
     */
    fun initializeNodesForUser(userId: UserId): List<ValueNode>

    // ==================== ValueEdge Operations ====================

    /**
     * Saves or updates a value edge.
     * Note: EdgeType cannot be changed after creation.
     */
    fun saveEdge(edge: ValueEdge): ValueEdge

    /**
     * Finds an edge by ID.
     */
    fun findEdgeById(id: ValueEdgeId): ValueEdge?

    /**
     * Finds an edge between two axes for a user.
     */
    fun findEdgeByUserAndAxes(
        userId: UserId,
        fromAxis: ValueAxis,
        toAxis: ValueAxis
    ): ValueEdge?

    /**
     * Finds all edges for a user.
     */
    fun findEdgesByUserId(userId: UserId): List<ValueEdge>

    /**
     * Finds all edges connected to a specific axis.
     */
    fun findEdgesByAxis(userId: UserId, axis: ValueAxis): List<ValueEdge>

    /**
     * Finds all conflict edges for a user.
     */
    fun findConflictEdges(userId: UserId): List<ValueEdge>

    /**
     * Finds all support edges for a user.
     */
    fun findSupportEdges(userId: UserId): List<ValueEdge>

    // ==================== Aggregate Operations ====================

    /**
     * Returns the complete value graph for a user.
     */
    fun findValueGraph(userId: UserId): ValueGraph?

    /**
     * Checks if a user has an initialized value graph.
     */
    fun hasValueGraph(userId: UserId): Boolean
}

/**
 * Aggregate object representing a user's complete value graph.
 */
data class ValueGraph(
    val userId: UserId,
    val nodes: List<ValueNode>,
    val edges: List<ValueEdge>
) {
    /**
     * Gets a node by axis.
     */
    fun getNode(axis: ValueAxis): ValueNode? =
        nodes.find { it.axis == axis }

    /**
     * Gets all conflict edges.
     */
    val conflicts: List<ValueEdge>
        get() = edges.filter { it.isConflict }

    /**
     * Gets all support edges.
     */
    val supports: List<ValueEdge>
        get() = edges.filter { it.isSupport }

    /**
     * Gets the top N values by positive association.
     */
    fun topPositiveValues(n: Int = 3): List<ValueNode> =
        nodes.filter { it.avgValence > 0 }
            .sortedByDescending { it.avgValence }
            .take(n)

    /**
     * Gets the top N values by negative association.
     */
    fun topNegativeValues(n: Int = 3): List<ValueNode> =
        nodes.filter { it.avgValence < 0 }
            .sortedBy { it.avgValence }
            .take(n)
}
