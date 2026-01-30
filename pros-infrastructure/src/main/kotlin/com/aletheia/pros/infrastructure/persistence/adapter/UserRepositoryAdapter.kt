package com.aletheia.pros.infrastructure.persistence.adapter

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.user.User
import com.aletheia.pros.domain.user.UserRepository
import com.aletheia.pros.infrastructure.persistence.mapper.UserMapper
import com.aletheia.pros.infrastructure.persistence.repository.JpaUserRepository
import org.springframework.stereotype.Repository

/**
 * Adapter implementing UserRepository using JPA.
 */
@Repository
class UserRepositoryAdapter(
    private val jpaRepository: JpaUserRepository,
    private val mapper: UserMapper
) : UserRepository {

    override fun save(user: User): User {
        val entity = mapper.toEntity(user)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
    }

    override fun findById(id: UserId): User? {
        return jpaRepository.findById(id.value)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    override fun findByEmail(email: String): User? {
        return jpaRepository.findByEmail(email.lowercase())
            ?.let { mapper.toDomain(it) }
    }

    override fun existsByEmail(email: String): Boolean {
        return jpaRepository.existsByEmail(email.lowercase())
    }
}
