package com.aletheia.pros.infrastructure.persistence

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.decision.Decision
import com.aletheia.pros.domain.decision.DecisionResult
import com.aletheia.pros.infrastructure.persistence.adapter.DecisionRepositoryAdapter
import com.aletheia.pros.infrastructure.persistence.mapper.DecisionMapper
import com.aletheia.pros.infrastructure.persistence.repository.JpaDecisionFeedbackRepository
import com.aletheia.pros.infrastructure.persistence.repository.JpaDecisionRepository
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
 * Integration tests for DecisionRepositoryAdapter using Testcontainers.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DecisionMapper::class)
@DisplayName("DecisionRepository Integration Tests")
class DecisionRepositoryIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg17")
        ).apply {
            withDatabaseName("pros_test")
            withUsername("test")
            withPassword("test")
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
    private lateinit var decisionRepository: JpaDecisionRepository

    @Autowired
    private lateinit var feedbackRepository: JpaDecisionFeedbackRepository

    @Autowired
    private lateinit var mapper: DecisionMapper

    private lateinit var repository: DecisionRepositoryAdapter

    private val userId = UserId.generate()

    @BeforeEach
    fun setUp() {
        repository = DecisionRepositoryAdapter(decisionRepository, feedbackRepository, mapper)
        feedbackRepository.deleteAll()
        decisionRepository.deleteAll()
    }

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        @Test
        fun `should apply offset-based pagination for non-aligned offsets`() {
            // Given
            val baseTime = Instant.parse("2025-01-01T00:00:00Z")
            val decisions = (0 until 30).map { index ->
                createDecision(
                    title = "decision-$index",
                    createdAt = baseTime.minusSeconds(index.toLong())
                )
            }
            decisions.forEach { repository.save(it) }

            // When
            val results = repository.findByUserId(userId, limit = 20, offset = 5)

            // Then
            val expected = (5 until 25).map { "decision-$it" }
            assertThat(results).hasSize(20)
            assertThat(results.map { it.title })
                .containsExactlyElementsOf(expected)
        }

        @Test
        fun `should return remaining items when offset crosses page boundary`() {
            // Given
            val baseTime = Instant.parse("2025-01-01T00:00:00Z")
            val decisions = (0 until 30).map { index ->
                createDecision(
                    title = "decision-$index",
                    createdAt = baseTime.minusSeconds(index.toLong())
                )
            }
            decisions.forEach { repository.save(it) }

            // When
            val results = repository.findByUserId(userId, limit = 20, offset = 25)

            // Then
            val expected = (25 until 30).map { "decision-$it" }
            assertThat(results).hasSize(5)
            assertThat(results.map { it.title })
                .containsExactlyElementsOf(expected)
        }
    }

    private fun createDecision(
        title: String,
        createdAt: Instant
    ): Decision {
        val result = DecisionResult.compute(
            fitA = 0.6,
            fitB = 0.4,
            regretA = 0.2,
            regretB = 0.3,
            lambda = 0.5,
            evidenceIds = emptyList(),
            valueAlignment = emptyMap()
        )

        return Decision.create(
            userId = userId,
            title = title,
            optionA = "Option A",
            optionB = "Option B",
            priorityAxis = null,
            result = result,
            createdAt = createdAt
        )
    }
}
