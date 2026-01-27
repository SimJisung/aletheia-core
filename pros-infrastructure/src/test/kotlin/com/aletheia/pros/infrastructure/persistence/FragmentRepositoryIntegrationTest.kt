package com.aletheia.pros.infrastructure.persistence

import com.aletheia.pros.domain.common.Embedding
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.fragment.ThoughtFragment
import com.aletheia.pros.infrastructure.persistence.adapter.FragmentRepositoryAdapter
import com.aletheia.pros.infrastructure.persistence.mapper.FragmentMapper
import com.aletheia.pros.infrastructure.persistence.repository.JpaThoughtFragmentRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Instant

/**
 * Integration tests for FragmentRepositoryAdapter using Testcontainers.
 *
 * These tests verify:
 * - Append-only storage principle
 * - Soft delete behavior
 * - Vector similarity search with pgvector
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(FragmentMapper::class)
@DisplayName("FragmentRepository Integration Tests")
class FragmentRepositoryIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg17")
        ).apply {
            withDatabaseName("pros_test")
            withUsername("test")
            withPassword("test")
            // pgvector extension will be created by Flyway migration
        }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.flyway.enabled") { "true" }
        }
    }

    @Autowired
    private lateinit var jpaRepository: JpaThoughtFragmentRepository

    @Autowired
    private lateinit var mapper: FragmentMapper

    private lateinit var repository: FragmentRepositoryAdapter

    private val userId = UserId.generate()
    private val testEmbedding = Embedding(FloatArray(1536) { 0.1f })

    @BeforeEach
    fun setUp() {
        repository = FragmentRepositoryAdapter(jpaRepository, mapper)
        // Clean up before each test
        jpaRepository.deleteAll()
    }

    @Nested
    @DisplayName("Save and Find Operations")
    inner class SaveAndFind {

        @Test
        fun `should save and retrieve fragment`() {
            // Given
            val fragment = createFragment("테스트 생각")

            // When
            val saved = repository.save(fragment)
            val found = repository.findById(saved.id)

            // Then
            assertThat(found).isNotNull
            assertThat(found?.textRaw).isEqualTo("테스트 생각")
            assertThat(found?.userId).isEqualTo(userId)
        }

        @Test
        fun `should preserve original text exactly - append-only principle`() {
            // Given
            val originalText = "  공백 포함 텍스트  \n줄바꿈도 있음  "
            val fragment = createFragment(originalText)

            // When
            val saved = repository.save(fragment)
            val found = repository.findById(saved.id)

            // Then - text must be preserved exactly as-is
            assertThat(found?.textRaw).isEqualTo(originalText)
        }

        @Test
        fun `should find fragments by user ID`() {
            // Given
            val fragment1 = createFragment("첫 번째 생각")
            val fragment2 = createFragment("두 번째 생각")
            val otherUser = UserId.generate()
            val otherFragment = createFragment("다른 사용자", otherUser)

            repository.save(fragment1)
            repository.save(fragment2)
            repository.save(otherFragment)

            // When
            val userFragments = repository.findByUserId(userId, limit = 10, offset = 0)

            // Then
            assertThat(userFragments).hasSize(2)
            assertThat(userFragments.map { it.textRaw })
                .containsExactlyInAnyOrder("첫 번째 생각", "두 번째 생각")
        }
    }

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        @Test
        fun `should apply offset-based pagination for non-aligned offsets`() {
            // Given
            val baseTime = Instant.parse("2025-01-01T00:00:00Z")
            val fragments = (0 until 30).map { index ->
                createFragment(
                    text = "fragment-$index",
                    createdAt = baseTime.minusSeconds(index.toLong())
                )
            }
            fragments.forEach { repository.save(it) }

            // When
            val results = repository.findByUserId(userId, limit = 20, offset = 5)

            // Then
            val expected = (5 until 25).map { "fragment-$it" }
            assertThat(results).hasSize(20)
            assertThat(results.map { it.textRaw })
                .containsExactlyElementsOf(expected)
        }

        @Test
        fun `should return remaining items when offset crosses page boundary`() {
            // Given
            val baseTime = Instant.parse("2025-01-01T00:00:00Z")
            val fragments = (0 until 30).map { index ->
                createFragment(
                    text = "fragment-$index",
                    createdAt = baseTime.minusSeconds(index.toLong())
                )
            }
            fragments.forEach { repository.save(it) }

            // When
            val results = repository.findByUserId(userId, limit = 20, offset = 25)

            // Then
            val expected = (25 until 30).map { "fragment-$it" }
            assertThat(results).hasSize(5)
            assertThat(results.map { it.textRaw })
                .containsExactlyElementsOf(expected)
        }
    }

    @Nested
    @DisplayName("Soft Delete Behavior")
    inner class SoftDelete {

        @Test
        fun `should soft delete fragment - data preserved`() {
            // Given
            val fragment = createFragment("삭제될 생각")
            val saved = repository.save(fragment)

            // When
            val deleted = repository.softDelete(saved.id)

            // Then
            assertThat(deleted).isTrue()

            // Fragment should still exist in database but marked as deleted
            val found = repository.findById(saved.id)
            assertThat(found?.isDeleted).isTrue()
            assertThat(found?.textRaw).isEqualTo("삭제될 생각") // Data preserved
        }

        @Test
        fun `should exclude deleted fragments from user queries`() {
            // Given
            val fragment1 = createFragment("유지될 생각")
            val fragment2 = createFragment("삭제될 생각")
            repository.save(fragment1)
            val toDelete = repository.save(fragment2)

            // When
            repository.softDelete(toDelete.id)
            val userFragments = repository.findByUserId(userId, limit = 10, offset = 0)

            // Then - only non-deleted fragments returned
            assertThat(userFragments).hasSize(1)
            assertThat(userFragments[0].textRaw).isEqualTo("유지될 생각")
        }

        @Test
        fun `should be idempotent - multiple deletes same result`() {
            // Given
            val fragment = createFragment("테스트")
            val saved = repository.save(fragment)

            // When
            val firstDelete = repository.softDelete(saved.id)
            val secondDelete = repository.softDelete(saved.id)

            // Then
            assertThat(firstDelete).isTrue()
            assertThat(secondDelete).isTrue() // Idempotent
        }
    }

    @Nested
    @DisplayName("Vector Similarity Search")
    inner class VectorSearch {

        @Test
        fun `should find similar fragments by embedding`() {
            // Given
            val similarEmbedding = Embedding(FloatArray(1536) { 0.1f })
            val differentEmbedding = Embedding(FloatArray(1536) { if (it < 768) 1.0f else -1.0f })

            val similar1 = createFragment("비슷한 생각 1", embedding = similarEmbedding)
            val similar2 = createFragment("비슷한 생각 2", embedding = similarEmbedding)
            val different = createFragment("다른 생각", embedding = differentEmbedding)

            repository.save(similar1)
            repository.save(similar2)
            repository.save(different)

            // When
            val queryEmbedding = Embedding(FloatArray(1536) { 0.1f })
            val results = repository.findSimilarByEmbedding(
                userId = userId,
                embedding = queryEmbedding,
                limit = 2,
                minSimilarity = 0.5
            )

            // Then
            assertThat(results).hasSize(2)
            assertThat(results.map { it.textRaw })
                .containsExactlyInAnyOrder("비슷한 생각 1", "비슷한 생각 2")
        }

        @Test
        fun `should respect similarity threshold`() {
            // Given
            val fragment = createFragment("테스트", embedding = testEmbedding)
            repository.save(fragment)

            val veryDifferent = Embedding(FloatArray(1536) { if (it % 2 == 0) 1.0f else -1.0f })

            // When - high threshold should filter out dissimilar results
            val results = repository.findSimilarByEmbedding(
                userId = userId,
                embedding = veryDifferent,
                limit = 10,
                minSimilarity = 0.9
            )

            // Then
            assertThat(results).isEmpty()
        }
    }

    @Nested
    @DisplayName("Time Range Queries")
    inner class TimeRangeQueries {

        @Test
        fun `should find fragments within time range`() {
            // Given
            val fragment1 = createFragment("과거 생각")
            val fragment2 = createFragment("최근 생각")

            val saved1 = repository.save(fragment1)
            Thread.sleep(100) // Small delay
            val saved2 = repository.save(fragment2)

            // When
            val from = saved1.createdAt
            val to = saved2.createdAt.plusSeconds(1)
            val results = repository.findByUserIdAndTimeRange(userId, from, to)

            // Then
            assertThat(results).hasSize(2)
        }
    }

    // Helper methods
    private fun createFragment(
        text: String,
        user: UserId = userId,
        embedding: Embedding = testEmbedding,
        createdAt: Instant = Instant.now()
    ): ThoughtFragment {
        return ThoughtFragment.create(
            userId = user,
            textRaw = text,
            moodValence = 0.5,
            arousal = 0.5,
            embedding = embedding,
            createdAt = createdAt
        )
    }
}
