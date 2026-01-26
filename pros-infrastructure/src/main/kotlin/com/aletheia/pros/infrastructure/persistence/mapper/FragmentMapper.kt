package com.aletheia.pros.infrastructure.persistence.mapper

import com.aletheia.pros.domain.common.Embedding
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.fragment.Arousal
import com.aletheia.pros.domain.fragment.MoodValence
import com.aletheia.pros.domain.fragment.ThoughtFragment
import com.aletheia.pros.infrastructure.persistence.entity.ThoughtFragmentEntity
import org.springframework.stereotype.Component

/**
 * Mapper for converting between ThoughtFragment domain model and JPA entity.
 */
@Component
class FragmentMapper {

    /**
     * Converts a domain ThoughtFragment to a JPA entity.
     */
    fun toEntity(domain: ThoughtFragment): ThoughtFragmentEntity {
        return ThoughtFragmentEntity(
            id = domain.id.value,
            userId = domain.userId.value,
            textRaw = domain.textRaw,
            createdAt = domain.createdAt,
            moodValence = domain.moodValence.value,
            arousal = domain.arousal.value,
            topicHint = domain.topicHint,
            embedding = domain.embedding?.values,
            deletedAt = domain.deletedAt
        )
    }

    /**
     * Converts a JPA entity to a domain ThoughtFragment.
     */
    fun toDomain(entity: ThoughtFragmentEntity): ThoughtFragment {
        return ThoughtFragment(
            id = FragmentId(entity.id),
            userId = UserId(entity.userId),
            textRaw = entity.textRaw,
            createdAt = entity.createdAt,
            moodValence = MoodValence(entity.moodValence),
            arousal = Arousal(entity.arousal),
            topicHint = entity.topicHint,
            embedding = entity.embedding?.let { Embedding(it) },
            deletedAt = entity.deletedAt
        )
    }

    /**
     * Converts a list of entities to domain models.
     */
    fun toDomainList(entities: List<ThoughtFragmentEntity>): List<ThoughtFragment> {
        return entities.map { toDomain(it) }
    }

    /**
     * Converts an embedding to a PostgreSQL vector string format.
     */
    fun embeddingToString(embedding: Embedding): String {
        return embedding.values.joinToString(",", "[", "]")
    }

    /**
     * Parses an embedding from a PostgreSQL vector string format.
     */
    fun stringToEmbedding(vectorString: String): Embedding {
        val values = vectorString
            .trim('[', ']')
            .split(",")
            .map { it.trim().toFloat() }
            .toFloatArray()
        return Embedding(values)
    }
}
