package com.aletheia.pros.api.dto.response

import com.aletheia.pros.domain.value.ValueEdge
import com.aletheia.pros.domain.value.ValueGraph
import com.aletheia.pros.domain.value.ValueNode

/**
 * Response for the user's value graph.
 */
data class ValueGraphResponse(
    val nodes: List<ValueNodeResponse>,
    val edges: List<ValueEdgeResponse>
) {
    companion object {
        fun from(graph: ValueGraph): ValueGraphResponse {
            return ValueGraphResponse(
                nodes = graph.nodes.map { ValueNodeResponse.from(it) },
                edges = graph.edges.map { ValueEdgeResponse.from(it) }
            )
        }
    }
}

/**
 * Response for a value node.
 */
data class ValueNodeResponse(
    val axis: String,
    val displayName: String,
    val avgValence: Double,
    val recentTrend: String,
    val fragmentCount: Double
) {
    companion object {
        fun from(node: ValueNode): ValueNodeResponse {
            return ValueNodeResponse(
                axis = node.axis.name,
                displayName = node.axis.displayNameKo,
                avgValence = node.avgValence,
                recentTrend = node.recentTrend.name,
                fragmentCount = node.fragmentCount
            )
        }
    }
}

/**
 * Response for a value edge.
 */
data class ValueEdgeResponse(
    val fromAxis: String,
    val toAxis: String,
    val edgeType: String,
    val weight: Double
) {
    companion object {
        fun from(edge: ValueEdge): ValueEdgeResponse {
            return ValueEdgeResponse(
                fromAxis = edge.fromAxis.name,
                toAxis = edge.toAxis.name,
                edgeType = edge.edgeType.name,
                weight = edge.weight
            )
        }
    }
}
