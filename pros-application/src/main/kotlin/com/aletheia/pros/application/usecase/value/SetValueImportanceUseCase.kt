package com.aletheia.pros.application.usecase.value

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.value.ValueAxis
import com.aletheia.pros.domain.value.ValueImportance
import com.aletheia.pros.domain.value.ValueImportanceRepository

/**
 * Use case for setting user's value importance ratings.
 */
class SetValueImportanceUseCase(
    private val repository: ValueImportanceRepository
) {

    /**
     * Sets or updates importance ratings.
     *
     * @param command Contains userId and importance values (1-10 scale)
     * @return Updated ValueImportance
     */
    fun execute(command: SetValueImportanceCommand): ValueImportance {
        val existing = repository.findByUserId(command.userId)

        val importance = if (existing != null) {
            // Normalize and update existing
            val normalizedMap = command.importanceValues.mapValues { (_, value) ->
                ValueImportance.normalizeFromScale(value)
            }
            existing.update(normalizedMap)
        } else {
            // Create new
            ValueImportance.create(
                userId = command.userId,
                importanceMap = command.importanceValues
            )
        }

        return repository.save(importance)
    }
}

/**
 * Command for setting value importance.
 */
data class SetValueImportanceCommand(
    val userId: UserId,
    val importanceValues: Map<ValueAxis, Double>  // 1-10 scale
) {
    init {
        importanceValues.forEach { (axis, value) ->
            require(value in 1.0..10.0) {
                "Importance for $axis must be between 1 and 10, got: $value"
            }
        }
    }
}
