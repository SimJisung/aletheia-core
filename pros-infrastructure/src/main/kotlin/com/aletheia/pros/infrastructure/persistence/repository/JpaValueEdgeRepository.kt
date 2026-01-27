package com.aletheia.pros.infrastructure.persistence.repository

import com.aletheia.pros.infrastructure.persistence.entity.ValueEdgeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA Repository for ValueEdgeEntity.
 *
 * IMPORTANT: CONFLICT edges are NEVER deleted.
 */
@Repository
interface JpaValueEdgeRepository : JpaRepository<ValueEdgeEntity, UUID> {

    /**
     * Finds all edges for a user.
     */
    fun findByUserId(userId: UUID): List<ValueEdgeEntity>

    /**
     * Finds an edge by user and axes.
     */
    @Query("""
        SELECT e FROM ValueEdgeEntity e
        WHERE e.userId = :userId
          AND e.fromAxis = :fromAxis
          AND e.toAxis = :toAxis
    """)
    fun findByUserIdAndAxes(
        @Param("userId") userId: UUID,
        @Param("fromAxis") fromAxis: String,
        @Param("toAxis") toAxis: String
    ): ValueEdgeEntity?

    /**
     * Finds all edges connected to a specific axis.
     */
    @Query("""
        SELECT e FROM ValueEdgeEntity e
        WHERE e.userId = :userId
          AND (e.fromAxis = :axis OR e.toAxis = :axis)
    """)
    fun findByUserIdAndAxis(
        @Param("userId") userId: UUID,
        @Param("axis") axis: String
    ): List<ValueEdgeEntity>

    /**
     * Finds all conflict edges for a user.
     */
    @Query("""
        SELECT e FROM ValueEdgeEntity e
        WHERE e.userId = :userId AND e.edgeType = 'CONFLICT'
    """)
    fun findConflictEdges(@Param("userId") userId: UUID): List<ValueEdgeEntity>

    /**
     * Finds all support edges for a user.
     */
    @Query("""
        SELECT e FROM ValueEdgeEntity e
        WHERE e.userId = :userId AND e.edgeType = 'SUPPORT'
    """)
    fun findSupportEdges(@Param("userId") userId: UUID): List<ValueEdgeEntity>
}
