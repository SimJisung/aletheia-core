package com.aletheia.pros.infrastructure.persistence.repository

import com.aletheia.pros.infrastructure.persistence.entity.ValueImportanceEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * JPA Repository for ValueImportance entities.
 */
interface JpaValueImportanceRepository : JpaRepository<ValueImportanceEntity, UUID> {

    /**
     * Finds the importance record for a user.
     */
    fun findByUserId(userId: UUID): ValueImportanceEntity?

    /**
     * Finds all importance records for a user ordered by version descending.
     */
    fun findAllByUserIdOrderByVersionDesc(userId: UUID): List<ValueImportanceEntity>

    /**
     * Checks if a user has any importance settings.
     */
    fun existsByUserId(userId: UUID): Boolean
}
