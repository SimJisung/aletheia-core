package com.aletheia.pros.infrastructure.persistence.repository

import com.aletheia.pros.infrastructure.persistence.entity.ValueNodeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA Repository for ValueNodeEntity.
 */
@Repository
interface JpaValueNodeRepository : JpaRepository<ValueNodeEntity, UUID> {

    /**
     * Finds all value nodes for a user.
     */
    fun findByUserId(userId: UUID): List<ValueNodeEntity>

    /**
     * Finds a value node by user and axis.
     */
    @Query("""
        SELECT n FROM ValueNodeEntity n
        WHERE n.userId = :userId AND n.axis = :axis
    """)
    fun findByUserIdAndAxis(
        @Param("userId") userId: UUID,
        @Param("axis") axis: String
    ): ValueNodeEntity?

    /**
     * Checks if a user has value nodes initialized.
     */
    @Query("""
        SELECT COUNT(n) > 0 FROM ValueNodeEntity n
        WHERE n.userId = :userId
    """)
    fun existsByUserId(@Param("userId") userId: UUID): Boolean

    /**
     * Counts nodes for a user.
     */
    fun countByUserId(userId: UUID): Long
}
