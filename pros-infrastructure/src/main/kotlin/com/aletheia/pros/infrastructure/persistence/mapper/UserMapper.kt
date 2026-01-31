package com.aletheia.pros.infrastructure.persistence.mapper

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.user.User
import com.aletheia.pros.infrastructure.persistence.entity.UserEntity
import org.springframework.stereotype.Component

/**
 * Mapper for User domain <-> entity conversion.
 */
@Component
class UserMapper {

    fun toEntity(domain: User): UserEntity {
        return UserEntity(
            id = domain.id.value,
            email = domain.email,
            passwordHash = domain.passwordHash,
            name = domain.name,
            avatarUrl = domain.avatarUrl,
            createdAt = domain.createdAt,
            lastLoginAt = domain.lastLoginAt,
            isActive = domain.isActive
        )
    }

    fun toDomain(entity: UserEntity): User {
        return User(
            id = UserId(entity.id),
            email = entity.email,
            passwordHash = entity.passwordHash,
            name = entity.name,
            avatarUrl = entity.avatarUrl,
            createdAt = entity.createdAt,
            lastLoginAt = entity.lastLoginAt,
            isActive = entity.isActive
        )
    }
}
