package com.aletheia.pros.infrastructure.persistence.adapter

import com.aletheia.pros.domain.common.Embedding
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.fragment.Arousal
import com.aletheia.pros.domain.fragment.FragmentRepository
import com.aletheia.pros.domain.fragment.MoodValence
import com.aletheia.pros.domain.fragment.SimilarFragment
import com.aletheia.pros.domain.fragment.ThoughtFragment
import com.aletheia.pros.infrastructure.persistence.mapper.FragmentMapper
import com.aletheia.pros.infrastructure.persistence.repository.JpaThoughtFragmentRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Adapter implementing FragmentRepository using JPA.
 *
 * This adapter bridges the domain repository interface with Spring Data JPA.
 */
@Repository
@Transactional
class FragmentRepositoryAdapter(
    private val jpaRepository: JpaThoughtFragmentRepository,
    private val mapper: FragmentMapper
) : FragmentRepository {

    override fun save(fragment: ThoughtFragment): ThoughtFragment {
        val entity = mapper.toEntity(fragment)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
    }

    @Transactional(readOnly = true)
    override fun findById(id: FragmentId): ThoughtFragment? {
        return jpaRepository.findByIdNotDeleted(id.value)?.let { mapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findByIdIncludingDeleted(id: FragmentId): ThoughtFragment? {
        return jpaRepository.findByIdIncludingDeleted(id.value)?.let { mapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findByUserId(userId: UserId, limit: Int, offset: Int): List<ThoughtFragment> {
        val pageIndex = offset / limit
        val pageOffset = offset % limit
        val page = jpaRepository.findByUserIdNotDeleted(userId.value, PageRequest.of(pageIndex, limit))
        if (pageOffset == 0) {
            return mapper.toDomainList(page.content)
        }
        if (page.content.size <= pageOffset) {
            return emptyList()
        }

        val firstPageContent = page.content.drop(pageOffset)
        if (!page.hasNext()) {
            return mapper.toDomainList(firstPageContent)
        }

        val nextPage = jpaRepository.findByUserIdNotDeleted(
            userId.value,
            PageRequest.of(pageIndex + 1, limit)
        )
        val combined = firstPageContent + nextPage.content
        return mapper.toDomainList(combined.take(limit))
    }

    @Transactional(readOnly = true)
    override fun findByUserIdAndTimeRange(
        userId: UserId,
        from: Instant,
        to: Instant
    ): List<ThoughtFragment> {
        val entities = jpaRepository.findByUserIdAndTimeRange(userId.value, from, to)
        return mapper.toDomainList(entities)
    }

    @Transactional(readOnly = true)
    override fun countByUserId(userId: UserId): Long {
        return jpaRepository.countByUserIdNotDeleted(userId.value)
    }

    override fun softDelete(id: FragmentId, deletedAt: Instant): Boolean {
        return jpaRepository.softDelete(id.value, deletedAt) > 0
    }

    override fun updateEmbedding(id: FragmentId, embedding: Embedding): Boolean {
        val embeddingStr = mapper.embeddingToString(embedding)
        return jpaRepository.updateEmbedding(id.value, embeddingStr) > 0
    }

    @Transactional(readOnly = true)
    override fun findSimilar(
        userId: UserId,
        queryEmbedding: Embedding,
        topK: Int,
        minSimilarity: Double
    ): List<SimilarFragment> {
        val embeddingStr = mapper.embeddingToString(queryEmbedding)
        val results = jpaRepository.findSimilarFragments(
            userId.value,
            embeddingStr,
            topK,
            minSimilarity
        )

        return results.map { row ->
            val fragment = ThoughtFragment(
                id = FragmentId(row[0] as UUID),
                userId = UserId(row[1] as UUID),
                textRaw = row[2] as String,
                createdAt = (row[3] as java.sql.Timestamp).toInstant(),
                moodValence = MoodValence(row[4] as Double),
                arousal = Arousal(row[5] as Double),
                topicHint = row[6] as? String,
                embedding = (row[7] as? FloatArray)?.let { Embedding(it) },
                deletedAt = row[8] as? Instant
            )
            val similarity = (row[9] as Number).toDouble()
            SimilarFragment(fragment, similarity)
        }
    }

    @Transactional(readOnly = true)
    override fun exists(id: FragmentId): Boolean {
        return jpaRepository.existsByIdNotDeleted(id.value)
    }
}
