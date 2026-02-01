package com.aletheia.pros.infrastructure.persistence.mapper

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueImportanceId
import com.aletheia.pros.domain.value.ValueAxis
import com.aletheia.pros.domain.value.ValueImportance
import com.aletheia.pros.infrastructure.persistence.entity.ValueImportanceEntity
import org.springframework.stereotype.Component

/**
 * Mapper for converting between ValueImportance domain model and JPA entity.
 */
@Component
class ValueImportanceMapper {

    /**
     * Converts domain model to JPA entity.
     */
    fun toEntity(domain: ValueImportance): ValueImportanceEntity {
        return ValueImportanceEntity(
            id = domain.id.value,
            userId = domain.userId.value,
            importanceMap = domain.importanceMap.mapKeys { it.key.name },
            version = domain.version,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    /**
     * Converts JPA entity to domain model.
     */
    fun toDomain(entity: ValueImportanceEntity): ValueImportance {
        val importanceMap = entity.importanceMap.mapNotNull { (key, value) ->
            ValueAxis.fromName(key)?.let { axis -> axis to value }
        }.toMap()

        return ValueImportance(
            id = ValueImportanceId(entity.id),
            userId = UserId(entity.userId),
            importanceMap = importanceMap,
            version = entity.version,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
