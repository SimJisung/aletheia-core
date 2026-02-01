package com.aletheia.pros.infrastructure.persistence.adapter

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueImportanceId
import com.aletheia.pros.domain.value.ValueImportance
import com.aletheia.pros.domain.value.ValueImportanceRepository
import com.aletheia.pros.infrastructure.persistence.mapper.ValueImportanceMapper
import com.aletheia.pros.infrastructure.persistence.repository.JpaValueImportanceRepository
import org.springframework.stereotype.Repository

/**
 * Repository adapter implementing ValueImportanceRepository using JPA.
 */
@Repository
class ValueImportanceRepositoryAdapter(
    private val jpaRepository: JpaValueImportanceRepository,
    private val mapper: ValueImportanceMapper
) : ValueImportanceRepository {

    override fun save(importance: ValueImportance): ValueImportance {
        val entity = mapper.toEntity(importance)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
    }

    override fun findByUserId(userId: UserId): ValueImportance? {
        return jpaRepository.findByUserId(userId.value)?.let { mapper.toDomain(it) }
    }

    override fun findById(id: ValueImportanceId): ValueImportance? {
        return jpaRepository.findById(id.value).orElse(null)?.let { mapper.toDomain(it) }
    }

    override fun findAllVersionsByUserId(userId: UserId): List<ValueImportance> {
        return jpaRepository.findAllByUserIdOrderByVersionDesc(userId.value)
            .map { mapper.toDomain(it) }
    }

    override fun existsByUserId(userId: UserId): Boolean {
        return jpaRepository.existsByUserId(userId.value)
    }
}
