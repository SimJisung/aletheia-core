package com.aletheia.pros.domain.value

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueImportanceId

/**
 * Repository interface for ValueImportance persistence.
 */
interface ValueImportanceRepository {

    /**
     * Saves a value importance record.
     * Creates new or updates existing based on ID.
     */
    fun save(importance: ValueImportance): ValueImportance

    /**
     * Finds the current (latest version) importance for a user.
     */
    fun findByUserId(userId: UserId): ValueImportance?

    /**
     * Finds a specific importance record by ID.
     */
    fun findById(id: ValueImportanceId): ValueImportance?

    /**
     * Finds all versions of importance for a user (for audit).
     */
    fun findAllVersionsByUserId(userId: UserId): List<ValueImportance>

    /**
     * Checks if a user has any importance settings.
     */
    fun existsByUserId(userId: UserId): Boolean
}
