package com.aletheia.pros.domain.user

import com.aletheia.pros.domain.common.UserId

/**
 * Repository interface for User persistence.
 */
interface UserRepository {

    /**
     * Saves a user.
     */
    fun save(user: User): User

    /**
     * Finds a user by ID.
     */
    fun findById(id: UserId): User?

    /**
     * Finds a user by email.
     */
    fun findByEmail(email: String): User?

    /**
     * Checks if an email is already registered.
     */
    fun existsByEmail(email: String): Boolean
}
