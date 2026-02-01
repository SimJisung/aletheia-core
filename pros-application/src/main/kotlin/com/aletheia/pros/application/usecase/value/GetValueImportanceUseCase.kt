package com.aletheia.pros.application.usecase.value

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.value.ValueImportance
import com.aletheia.pros.domain.value.ValueImportanceRepository

/**
 * Use case for retrieving user's value importance ratings.
 */
class GetValueImportanceUseCase(
    private val repository: ValueImportanceRepository
) {

    /**
     * Gets the user's current importance settings.
     * Returns default if not explicitly set.
     */
    fun execute(userId: UserId): ValueImportance {
        return repository.findByUserId(userId)
            ?: ValueImportance.createDefault(userId)
    }
}
