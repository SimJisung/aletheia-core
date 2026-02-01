package com.aletheia.pros.application.usecase.user

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.user.User
import com.aletheia.pros.domain.user.UserRepository

/**
 * Use case for retrieving the current user's information.
 */
class GetCurrentUserUseCase(
    private val userRepository: UserRepository
) {

    /**
     * Retrieves a user by their ID.
     *
     * @param userId The ID of the user to retrieve
     * @return The user if found, null otherwise
     */
    fun execute(userId: UserId): User? {
        return userRepository.findById(userId)
    }
}
