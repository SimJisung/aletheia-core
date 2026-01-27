package com.aletheia.pros.infrastructure.persistence.mapper

import com.aletheia.pros.domain.common.Embedding
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.fragment.Arousal
import com.aletheia.pros.domain.fragment.MoodValence
import com.aletheia.pros.domain.fragment.ThoughtFragment
import com.aletheia.pros.infrastructure.persistence.entity.ThoughtFragmentEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@DisplayName("FragmentMapper Tests")
class FragmentMapperTest {

    private lateinit var mapper: FragmentMapper

    private val userId = UserId.generate()
    private val fragmentId = FragmentId.generate()
    private val createdAt = Instant.now()
    private val testEmbedding = Embedding(FloatArray(1536) { it * 0.001f })

    @BeforeEach
    fun setUp() {
        mapper = FragmentMapper()
    }

    @Nested
    @DisplayName("toEntity - Domain to Entity Mapping")
    inner class ToEntity {

        @Test
        fun `should convert domain fragment to entity`() {
            // Given
            val domain = ThoughtFragment(
                id = fragmentId,
                userId = userId,
                textRaw = "Test text",
                createdAt = createdAt,
                moodValence = MoodValence(0.7),
                arousal = Arousal(0.5),
                topicHint = "work",
                embedding = testEmbedding,
                deletedAt = null
            )

            // When
            val entity = mapper.toEntity(domain)

            // Then
            assertThat(entity.id).isEqualTo(fragmentId.value)
            assertThat(entity.userId).isEqualTo(userId.value)
            assertThat(entity.textRaw).isEqualTo("Test text")
            assertThat(entity.createdAt).isEqualTo(createdAt)
            assertThat(entity.moodValence).isEqualTo(0.7)
            assertThat(entity.arousal).isEqualTo(0.5)
            assertThat(entity.topicHint).isEqualTo("work")
            assertThat(entity.embedding).isEqualTo(testEmbedding.values)
            assertThat(entity.deletedAt).isNull()
        }

        @Test
        fun `should handle null embedding`() {
            // Given
            val domain = ThoughtFragment(
                id = fragmentId,
                userId = userId,
                textRaw = "Test text",
                createdAt = createdAt,
                moodValence = MoodValence(0.0),
                arousal = Arousal(0.5),
                topicHint = null,
                embedding = null,
                deletedAt = null
            )

            // When
            val entity = mapper.toEntity(domain)

            // Then
            assertThat(entity.embedding).isNull()
        }

        @Test
        fun `should handle null topic hint`() {
            // Given
            val domain = ThoughtFragment(
                id = fragmentId,
                userId = userId,
                textRaw = "Test text",
                createdAt = createdAt,
                moodValence = MoodValence(0.0),
                arousal = Arousal(0.5),
                topicHint = null,
                embedding = testEmbedding,
                deletedAt = null
            )

            // When
            val entity = mapper.toEntity(domain)

            // Then
            assertThat(entity.topicHint).isNull()
        }

        @Test
        fun `should handle soft-deleted fragment`() {
            // Given
            val deletedAt = Instant.now()
            val domain = ThoughtFragment(
                id = fragmentId,
                userId = userId,
                textRaw = "Test text",
                createdAt = createdAt,
                moodValence = MoodValence(0.0),
                arousal = Arousal(0.5),
                embedding = testEmbedding,
                deletedAt = deletedAt
            )

            // When
            val entity = mapper.toEntity(domain)

            // Then
            assertThat(entity.deletedAt).isEqualTo(deletedAt)
            assertThat(entity.isDeleted).isTrue()
        }

        @Test
        fun `should handle negative valence`() {
            // Given
            val domain = ThoughtFragment(
                id = fragmentId,
                userId = userId,
                textRaw = "Negative mood",
                createdAt = createdAt,
                moodValence = MoodValence(-0.8),
                arousal = Arousal(0.3),
                embedding = null,
                deletedAt = null
            )

            // When
            val entity = mapper.toEntity(domain)

            // Then
            assertThat(entity.moodValence).isEqualTo(-0.8)
        }
    }

    @Nested
    @DisplayName("toDomain - Entity to Domain Mapping")
    inner class ToDomain {

        @Test
        fun `should convert entity to domain fragment`() {
            // Given
            val entity = ThoughtFragmentEntity(
                id = fragmentId.value,
                userId = userId.value,
                textRaw = "Test text",
                createdAt = createdAt,
                moodValence = 0.7,
                arousal = 0.5,
                topicHint = "work",
                embedding = testEmbedding.values,
                deletedAt = null
            )

            // When
            val domain = mapper.toDomain(entity)

            // Then
            assertThat(domain.id.value).isEqualTo(fragmentId.value)
            assertThat(domain.userId.value).isEqualTo(userId.value)
            assertThat(domain.textRaw).isEqualTo("Test text")
            assertThat(domain.createdAt).isEqualTo(createdAt)
            assertThat(domain.moodValence.value).isEqualTo(0.7)
            assertThat(domain.arousal.value).isEqualTo(0.5)
            assertThat(domain.topicHint).isEqualTo("work")
            assertThat(domain.embedding?.values).isEqualTo(testEmbedding.values)
            assertThat(domain.deletedAt).isNull()
        }

        @Test
        fun `should handle null embedding`() {
            // Given
            val entity = ThoughtFragmentEntity(
                id = fragmentId.value,
                userId = userId.value,
                textRaw = "Test text",
                createdAt = createdAt,
                moodValence = 0.0,
                arousal = 0.5,
                topicHint = null,
                embedding = null,
                deletedAt = null
            )

            // When
            val domain = mapper.toDomain(entity)

            // Then
            assertThat(domain.embedding).isNull()
            assertThat(domain.hasEmbedding).isFalse()
        }

        @Test
        fun `should handle deleted entity`() {
            // Given
            val deletedAt = Instant.now()
            val entity = ThoughtFragmentEntity(
                id = fragmentId.value,
                userId = userId.value,
                textRaw = "Test text",
                createdAt = createdAt,
                moodValence = 0.0,
                arousal = 0.5,
                embedding = null,
                deletedAt = deletedAt
            )

            // When
            val domain = mapper.toDomain(entity)

            // Then
            assertThat(domain.deletedAt).isEqualTo(deletedAt)
            assertThat(domain.isDeleted).isTrue()
        }
    }

    @Nested
    @DisplayName("toDomainList - Batch Entity to Domain Mapping")
    inner class ToDomainList {

        @Test
        fun `should convert list of entities to domain list`() {
            // Given
            val entities = listOf(
                ThoughtFragmentEntity(
                    id = UUID.randomUUID(),
                    userId = userId.value,
                    textRaw = "Text 1",
                    createdAt = createdAt,
                    moodValence = 0.5,
                    arousal = 0.5,
                    embedding = null
                ),
                ThoughtFragmentEntity(
                    id = UUID.randomUUID(),
                    userId = userId.value,
                    textRaw = "Text 2",
                    createdAt = createdAt,
                    moodValence = -0.3,
                    arousal = 0.7,
                    embedding = null
                )
            )

            // When
            val domains = mapper.toDomainList(entities)

            // Then
            assertThat(domains).hasSize(2)
            assertThat(domains[0].textRaw).isEqualTo("Text 1")
            assertThat(domains[1].textRaw).isEqualTo("Text 2")
        }

        @Test
        fun `should handle empty list`() {
            // When
            val domains = mapper.toDomainList(emptyList())

            // Then
            assertThat(domains).isEmpty()
        }
    }

    @Nested
    @DisplayName("Embedding String Conversion")
    inner class EmbeddingStringConversion {

        @Test
        fun `should convert embedding to PostgreSQL vector string`() {
            // Given
            val embedding = Embedding(floatArrayOf(0.1f, 0.2f, 0.3f))

            // When
            val vectorString = mapper.embeddingToString(embedding)

            // Then
            assertThat(vectorString).isEqualTo("[0.1,0.2,0.3]")
        }

        @Test
        fun `should parse embedding from PostgreSQL vector string`() {
            // Given
            val vectorString = "[0.1,0.2,0.3]"

            // When
            val embedding = mapper.stringToEmbedding(vectorString)

            // Then
            assertThat(embedding.values).hasSize(3)
            assertThat(embedding.values[0]).isEqualTo(0.1f)
            assertThat(embedding.values[1]).isEqualTo(0.2f)
            assertThat(embedding.values[2]).isEqualTo(0.3f)
        }

        @Test
        fun `should handle whitespace in vector string`() {
            // Given
            val vectorString = "[ 0.1 , 0.2 , 0.3 ]"

            // When
            val embedding = mapper.stringToEmbedding(vectorString)

            // Then
            assertThat(embedding.values).hasSize(3)
            assertThat(embedding.values[0]).isEqualTo(0.1f)
        }

        @Test
        fun `should roundtrip embedding through string conversion`() {
            // Given
            val original = Embedding(floatArrayOf(0.123f, 0.456f, 0.789f))

            // When
            val vectorString = mapper.embeddingToString(original)
            val restored = mapper.stringToEmbedding(vectorString)

            // Then
            assertThat(restored.values).hasSize(original.values.size)
            for (i in original.values.indices) {
                assertThat(restored.values[i]).isCloseTo(original.values[i], org.assertj.core.data.Offset.offset(0.0001f))
            }
        }
    }

    @Nested
    @DisplayName("Round-trip Conversion")
    inner class RoundTrip {

        @Test
        fun `should preserve data through domain to entity to domain conversion`() {
            // Given
            val original = ThoughtFragment(
                id = fragmentId,
                userId = userId,
                textRaw = "Round-trip test",
                createdAt = createdAt,
                moodValence = MoodValence(0.7),
                arousal = Arousal(0.3),
                topicHint = "test",
                embedding = testEmbedding,
                deletedAt = null
            )

            // When
            val entity = mapper.toEntity(original)
            val restored = mapper.toDomain(entity)

            // Then
            assertThat(restored.id).isEqualTo(original.id)
            assertThat(restored.userId).isEqualTo(original.userId)
            assertThat(restored.textRaw).isEqualTo(original.textRaw)
            assertThat(restored.createdAt).isEqualTo(original.createdAt)
            assertThat(restored.moodValence.value).isEqualTo(original.moodValence.value)
            assertThat(restored.arousal.value).isEqualTo(original.arousal.value)
            assertThat(restored.topicHint).isEqualTo(original.topicHint)
            assertThat(restored.embedding?.values).isEqualTo(original.embedding?.values)
            assertThat(restored.deletedAt).isEqualTo(original.deletedAt)
        }
    }
}
