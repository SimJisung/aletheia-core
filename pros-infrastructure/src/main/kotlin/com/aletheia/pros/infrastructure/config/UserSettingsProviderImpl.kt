package com.aletheia.pros.infrastructure.config

import com.aletheia.pros.application.usecase.decision.UserSettings
import com.aletheia.pros.application.usecase.decision.UserSettingsProvider
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.infrastructure.persistence.entity.UserSettingsEntity
import com.aletheia.pros.infrastructure.persistence.repository.JpaUserSettingsRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

/**
 * Implementation of UserSettingsProvider that retrieves settings from the database.
 *
 * Creates default settings if none exist for a user.
 */
@Component
class UserSettingsProviderImpl(
    private val repository: JpaUserSettingsRepository,
    @Value("\${pros.decision.default-lambda:1.0}")
    private val defaultLambda: Double,
    @Value("\${pros.decision.default-regret-prior:0.2}")
    private val defaultRegretPrior: Double
) : UserSettingsProvider {

    override suspend fun getSettings(userId: UserId): UserSettings {
        val entity = repository.findByUserId(userId.value)
            ?: createDefaultSettingsSafely(userId)

        return UserSettings(
            lambda = entity.lambda,
            regretPrior = entity.regretPrior
        )
    }

    private fun createDefaultSettingsSafely(userId: UserId): UserSettingsEntity {
        return try {
            createDefaultSettings(userId)
        } catch (ex: DataIntegrityViolationException) {
            repository.findByUserId(userId.value) ?: throw ex
        }
    }

    private fun createDefaultSettings(userId: UserId): UserSettingsEntity {
        val entity = UserSettingsEntity(
            id = UUID.randomUUID(),
            userId = userId.value,
            lambda = defaultLambda,
            regretPrior = defaultRegretPrior,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        return repository.save(entity)
    }

    /**
     * Updates lambda for a user based on feedback.
     */
    fun updateLambda(userId: UserId, newLambda: Double) {
        val entity = repository.findByUserId(userId.value)
            ?: throw IllegalArgumentException("User settings not found: $userId")

        entity.lambda = newLambda.coerceIn(0.1, 5.0)
        entity.updatedAt = Instant.now()
        repository.save(entity)
    }

    /**
     * Updates regret prior for a user based on historical data.
     */
    fun updateRegretPrior(userId: UserId, newPrior: Double) {
        val entity = repository.findByUserId(userId.value)
            ?: throw IllegalArgumentException("User settings not found: $userId")

        entity.regretPrior = newPrior.coerceIn(0.0, 1.0)
        entity.updatedAt = Instant.now()
        repository.save(entity)
    }
}
