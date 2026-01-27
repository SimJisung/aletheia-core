package com.aletheia.pros.infrastructure.persistence.mapper

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueEdgeId
import com.aletheia.pros.domain.common.ValueNodeId
import com.aletheia.pros.domain.value.*
import com.aletheia.pros.infrastructure.persistence.entity.ValueEdgeEntity
import com.aletheia.pros.infrastructure.persistence.entity.ValueNodeEntity
import org.springframework.stereotype.Component

/**
 * Mapper for converting between ValueGraph domain models and JPA entities.
 */
@Component
class ValueGraphMapper {

    // ==================== ValueNode Mapping ====================

    /**
     * Converts a domain ValueNode to a JPA entity.
     */
    fun toEntity(domain: ValueNode): ValueNodeEntity {
        return ValueNodeEntity(
            id = domain.id.value,
            userId = domain.userId.value,
            axis = domain.axis.name,
            avgValence = domain.avgValence,
            recentTrend = domain.recentTrend.name,
            fragmentCount = domain.fragmentCount,
            updatedAt = domain.updatedAt
        )
    }

    /**
     * Converts a JPA ValueNodeEntity to a domain model.
     */
    fun toDomain(entity: ValueNodeEntity): ValueNode {
        return ValueNode(
            id = ValueNodeId(entity.id),
            userId = UserId(entity.userId),
            axis = ValueAxis.valueOf(entity.axis),
            avgValence = entity.avgValence,
            recentTrend = Trend.valueOf(entity.recentTrend),
            fragmentCount = entity.fragmentCount,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Converts a list of node entities to domain models.
     */
    fun toNodeDomainList(entities: List<ValueNodeEntity>): List<ValueNode> {
        return entities.map { toDomain(it) }
    }

    // ==================== ValueEdge Mapping ====================

    /**
     * Converts a domain ValueEdge to a JPA entity.
     */
    fun toEntity(domain: ValueEdge): ValueEdgeEntity {
        return ValueEdgeEntity(
            id = domain.id.value,
            userId = domain.userId.value,
            fromAxis = domain.fromAxis.name,
            toAxis = domain.toAxis.name,
            edgeType = domain.edgeType.name,
            weight = domain.weight,
            updatedAt = domain.updatedAt
        )
    }

    /**
     * Converts a JPA ValueEdgeEntity to a domain model.
     */
    fun toDomain(entity: ValueEdgeEntity): ValueEdge {
        return ValueEdge(
            id = ValueEdgeId(entity.id),
            userId = UserId(entity.userId),
            fromAxis = ValueAxis.valueOf(entity.fromAxis),
            toAxis = ValueAxis.valueOf(entity.toAxis),
            edgeType = EdgeType.valueOf(entity.edgeType),
            weight = entity.weight,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * Converts a list of edge entities to domain models.
     */
    fun toEdgeDomainList(entities: List<ValueEdgeEntity>): List<ValueEdge> {
        return entities.map { toDomain(it) }
    }

    // ==================== ValueGraph Mapping ====================

    /**
     * Creates a ValueGraph from node and edge entities.
     */
    fun toValueGraph(
        userId: UserId,
        nodeEntities: List<ValueNodeEntity>,
        edgeEntities: List<ValueEdgeEntity>
    ): ValueGraph {
        return ValueGraph(
            userId = userId,
            nodes = toNodeDomainList(nodeEntities),
            edges = toEdgeDomainList(edgeEntities)
        )
    }
}
