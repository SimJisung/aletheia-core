package com.aletheia.pros.infrastructure.persistence.adapter

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueEdgeId
import com.aletheia.pros.domain.common.ValueNodeId
import com.aletheia.pros.domain.value.*
import com.aletheia.pros.infrastructure.persistence.mapper.ValueGraphMapper
import com.aletheia.pros.infrastructure.persistence.repository.JpaValueEdgeRepository
import com.aletheia.pros.infrastructure.persistence.repository.JpaValueNodeRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Adapter implementing ValueGraphRepository using JPA.
 */
@Repository
@Transactional
class ValueGraphRepositoryAdapter(
    private val nodeRepository: JpaValueNodeRepository,
    private val edgeRepository: JpaValueEdgeRepository,
    private val mapper: ValueGraphMapper
) : ValueGraphRepository {

    // ==================== ValueNode Operations ====================

    override fun saveNode(node: ValueNode): ValueNode {
        val entity = mapper.toEntity(node)
        val saved = nodeRepository.save(entity)
        return mapper.toDomain(saved)
    }

    @Transactional(readOnly = true)
    override fun findNodeById(id: ValueNodeId): ValueNode? {
        return nodeRepository.findById(id.value)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findNodeByUserAndAxis(userId: UserId, axis: ValueAxis): ValueNode? {
        return nodeRepository.findByUserIdAndAxis(userId.value, axis.name)
            ?.let { mapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findNodesByUserId(userId: UserId): List<ValueNode> {
        return mapper.toNodeDomainList(nodeRepository.findByUserId(userId.value))
    }

    override fun initializeNodesForUser(userId: UserId): List<ValueNode> {
        val now = Instant.now()
        val nodes = ValueNode.createAllForUser(userId, now)
        nodes.forEach { node ->
            nodeRepository.insertIfAbsent(
                id = node.id.value,
                userId = node.userId.value,
                axis = node.axis.name,
                avgValence = node.avgValence,
                recentTrend = node.recentTrend.name,
                fragmentCount = node.fragmentCount,
                updatedAt = node.updatedAt
            )
        }
        return mapper.toNodeDomainList(nodeRepository.findByUserId(userId.value))
    }

    // ==================== ValueEdge Operations ====================

    override fun saveEdge(edge: ValueEdge): ValueEdge {
        val entity = mapper.toEntity(edge)
        val saved = edgeRepository.save(entity)
        return mapper.toDomain(saved)
    }

    @Transactional(readOnly = true)
    override fun findEdgeById(id: ValueEdgeId): ValueEdge? {
        return edgeRepository.findById(id.value)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findEdgeByUserAndAxes(
        userId: UserId,
        fromAxis: ValueAxis,
        toAxis: ValueAxis
    ): ValueEdge? {
        return edgeRepository.findByUserIdAndAxes(userId.value, fromAxis.name, toAxis.name)
            ?.let { mapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findEdgesByUserId(userId: UserId): List<ValueEdge> {
        return mapper.toEdgeDomainList(edgeRepository.findByUserId(userId.value))
    }

    @Transactional(readOnly = true)
    override fun findEdgesByAxis(userId: UserId, axis: ValueAxis): List<ValueEdge> {
        return mapper.toEdgeDomainList(
            edgeRepository.findByUserIdAndAxis(userId.value, axis.name)
        )
    }

    @Transactional(readOnly = true)
    override fun findConflictEdges(userId: UserId): List<ValueEdge> {
        return mapper.toEdgeDomainList(edgeRepository.findConflictEdges(userId.value))
    }

    @Transactional(readOnly = true)
    override fun findSupportEdges(userId: UserId): List<ValueEdge> {
        return mapper.toEdgeDomainList(edgeRepository.findSupportEdges(userId.value))
    }

    // ==================== Aggregate Operations ====================

    @Transactional(readOnly = true)
    override fun findValueGraph(userId: UserId): ValueGraph? {
        val nodes = nodeRepository.findByUserId(userId.value)
        if (nodes.isEmpty()) return null

        val edges = edgeRepository.findByUserId(userId.value)
        return mapper.toValueGraph(userId, nodes, edges)
    }

    @Transactional(readOnly = true)
    override fun hasValueGraph(userId: UserId): Boolean {
        return nodeRepository.existsByUserId(userId.value)
    }
}
