package com.aletheia.pros.infrastructure.persistence.repository

import com.aletheia.pros.infrastructure.persistence.entity.UserSettingsEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA Repository for UserSettingsEntity.
 */
@Repository
interface JpaUserSettingsRepository : JpaRepository<UserSettingsEntity, UUID> {

    /**
     * Finds settings by user ID.
     */
    fun findByUserId(userId: UUID): UserSettingsEntity?

    /**
     * Checks if settings exist for a user.
     */
    fun existsByUserId(userId: UUID): Boolean
}
