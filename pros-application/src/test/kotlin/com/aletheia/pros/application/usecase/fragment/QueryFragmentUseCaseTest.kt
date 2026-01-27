package com.aletheia.pros.application.usecase.fragment

import com.aletheia.pros.application.port.input.ListFragmentsQuery
import com.aletheia.pros.application.port.input.SimilarFragmentsQuery
import com.aletheia.pros.application.port.output.EmbeddingPort
import com.aletheia.pros.domain.common.Embedding
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.fragment.Arousal
import com.aletheia.pros.domain.fragment.FragmentRepository
import com.aletheia.pros.domain.fragment.MoodValence
import com.aletheia.pros.domain.fragment.SimilarFragment
import com.aletheia.pros.domain.fragment.ThoughtFragment
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
@DisplayName("QueryFragmentUseCase Tests")
class QueryFragmentUseCaseTest {

    @MockK
    private lateinit var fragmentRepository: FragmentRepository

    @MockK
    private lateinit var embeddingPort: EmbeddingPort

    private lateinit var useCase: QueryFragmentUseCase

    private val userId = UserId.generate()
    private val testEmbedding = Embedding(FloatArray(1536) { 0.1f })

    @BeforeEach
    fun setUp() {
        useCase = QueryFragmentUseCase(
            fragmentRepository = fragmentRepository,
            embeddingPort = embeddingPort
        )
    }

    private fun createTestFragment(
        id: FragmentId = FragmentId.generate(),
        text: String = "Test fragment text",
        valence: Double = 0.5,
        arousal: Double = 0.5
    ): ThoughtFragment {
        return ThoughtFragment.create(
            userId = userId,
            textRaw = text,
            moodValence = MoodValence(valence),
            arousal = Arousal(arousal)
        ).withEmbedding(testEmbedding).copy(id = id)
    }

    @Nested
    @DisplayName("getById - Get Fragment by ID")
    inner class GetById {

        @Test
        fun `should return fragment when found`() = runBlocking {
            // Given
            val fragmentId = FragmentId.generate()
            val fragment = createTestFragment(id = fragmentId)

            every { fragmentRepository.findById(fragmentId) } returns fragment

            // When
            val result = useCase.getById(fragmentId)

            // Then
            assertThat(result).isNotNull
            assertThat(result?.id).isEqualTo(fragmentId)
            verify(exactly = 1) { fragmentRepository.findById(fragmentId) }
        }

        @Test
        fun `should return null when fragment not found`() = runBlocking {
            // Given
            val fragmentId = FragmentId.generate()

            every { fragmentRepository.findById(fragmentId) } returns null

            // When
            val result = useCase.getById(fragmentId)

            // Then
            assertThat(result).isNull()
        }

        @Test
        fun `should return null when fragment is soft-deleted`() = runBlocking {
            // Given
            val fragmentId = FragmentId.generate()

            // Repository returns null for soft-deleted fragments
            every { fragmentRepository.findById(fragmentId) } returns null

            // When
            val result = useCase.getById(fragmentId)

            // Then
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("list - List Fragments with Pagination")
    inner class ListFragments {

        @Test
        fun `should return paginated fragments`() = runBlocking {
            // Given
            val fragments = listOf(
                createTestFragment(text = "Fragment 1"),
                createTestFragment(text = "Fragment 2"),
                createTestFragment(text = "Fragment 3")
            )
            val query = ListFragmentsQuery(userId = userId, limit = 2, offset = 0)

            // Return one extra to indicate hasMore
            every { fragmentRepository.findByUserId(userId, 3, 0) } returns fragments
            every { fragmentRepository.countByUserId(userId) } returns 10

            // When
            val result = useCase.list(query)

            // Then
            assertThat(result.fragments).hasSize(2)
            assertThat(result.total).isEqualTo(10)
            assertThat(result.hasMore).isTrue()
        }

        @Test
        fun `should return hasMore false when no more fragments`() = runBlocking {
            // Given
            val fragments = listOf(
                createTestFragment(text = "Fragment 1"),
                createTestFragment(text = "Fragment 2")
            )
            val query = ListFragmentsQuery(userId = userId, limit = 5, offset = 0)

            // Return exactly 2 (less than limit+1=6), so hasMore is false
            every { fragmentRepository.findByUserId(userId, 6, 0) } returns fragments
            every { fragmentRepository.countByUserId(userId) } returns 2

            // When
            val result = useCase.list(query)

            // Then
            assertThat(result.fragments).hasSize(2)
            assertThat(result.hasMore).isFalse()
        }

        @Test
        fun `should handle empty result`() = runBlocking {
            // Given
            val query = ListFragmentsQuery(userId = userId, limit = 10, offset = 0)

            every { fragmentRepository.findByUserId(userId, 11, 0) } returns emptyList()
            every { fragmentRepository.countByUserId(userId) } returns 0

            // When
            val result = useCase.list(query)

            // Then
            assertThat(result.fragments).isEmpty()
            assertThat(result.total).isEqualTo(0)
            assertThat(result.hasMore).isFalse()
        }

        @Test
        fun `should use offset for pagination`() = runBlocking {
            // Given
            val fragments = listOf(
                createTestFragment(text = "Fragment 3"),
                createTestFragment(text = "Fragment 4")
            )
            val query = ListFragmentsQuery(userId = userId, limit = 2, offset = 2)

            every { fragmentRepository.findByUserId(userId, 3, 2) } returns fragments
            every { fragmentRepository.countByUserId(userId) } returns 10

            // When
            val result = useCase.list(query)

            // Then
            assertThat(result.fragments).hasSize(2)
            verify { fragmentRepository.findByUserId(userId, 3, 2) }
        }

        @Test
        fun `should use default values when not specified`() = runBlocking {
            // Given
            val query = ListFragmentsQuery(userId = userId) // defaults: limit=20, offset=0

            every { fragmentRepository.findByUserId(userId, 21, 0) } returns emptyList()
            every { fragmentRepository.countByUserId(userId) } returns 0

            // When
            useCase.list(query)

            // Then
            verify { fragmentRepository.findByUserId(userId, 21, 0) }
        }
    }

    @Nested
    @DisplayName("findSimilar - Semantic Similarity Search")
    inner class FindSimilar {

        @Test
        fun `should find similar fragments`() = runBlocking {
            // Given
            val queryText = "Find similar content"
            val queryEmbedding = Embedding(FloatArray(1536) { 0.2f })
            val fragment1 = createTestFragment(text = "Similar content 1")
            val fragment2 = createTestFragment(text = "Similar content 2")

            val similarFragments = listOf(
                SimilarFragment(fragment1, 0.95),
                SimilarFragment(fragment2, 0.85)
            )

            val query = SimilarFragmentsQuery(userId = userId, queryText = queryText, topK = 10)

            coEvery { embeddingPort.embed(queryText) } returns queryEmbedding
            every { fragmentRepository.findSimilar(userId, queryEmbedding, 10) } returns similarFragments

            // When
            val results = useCase.findSimilar(query)

            // Then
            assertThat(results).hasSize(2)
            assertThat(results[0].similarity).isEqualTo(0.95)
            assertThat(results[1].similarity).isEqualTo(0.85)
            coVerify(exactly = 1) { embeddingPort.embed(queryText) }
            verify(exactly = 1) { fragmentRepository.findSimilar(userId, queryEmbedding, 10) }
        }

        @Test
        fun `should return empty list when no similar fragments found`() = runBlocking {
            // Given
            val queryText = "Unique content"
            val queryEmbedding = Embedding(FloatArray(1536) { 0.3f })

            val query = SimilarFragmentsQuery(userId = userId, queryText = queryText, topK = 5)

            coEvery { embeddingPort.embed(queryText) } returns queryEmbedding
            every { fragmentRepository.findSimilar(userId, queryEmbedding, 5) } returns emptyList()

            // When
            val results = useCase.findSimilar(query)

            // Then
            assertThat(results).isEmpty()
        }

        @Test
        fun `should use specified topK value`() = runBlocking {
            // Given
            val queryText = "Search query"
            val queryEmbedding = Embedding(FloatArray(1536) { 0.1f })

            val query = SimilarFragmentsQuery(userId = userId, queryText = queryText, topK = 3)

            coEvery { embeddingPort.embed(queryText) } returns queryEmbedding
            every { fragmentRepository.findSimilar(userId, queryEmbedding, 3) } returns emptyList()

            // When
            useCase.findSimilar(query)

            // Then
            verify { fragmentRepository.findSimilar(userId, queryEmbedding, 3) }
        }

        @Test
        fun `should generate embedding before searching`() = runBlocking {
            // Given
            val queryText = "Test query"
            val queryEmbedding = Embedding(FloatArray(1536) { 0.5f })

            val query = SimilarFragmentsQuery(userId = userId, queryText = queryText)

            coEvery { embeddingPort.embed(queryText) } returns queryEmbedding
            every { fragmentRepository.findSimilar(any(), any(), any()) } returns emptyList()

            // When
            useCase.findSimilar(query)

            // Then - embedding should be generated first
            coVerify(exactly = 1) { embeddingPort.embed(queryText) }
            verify { fragmentRepository.findSimilar(userId, queryEmbedding, 10) }
        }

        @Test
        fun `should return results in order of similarity`() = runBlocking {
            // Given
            val queryText = "Search"
            val queryEmbedding = Embedding(FloatArray(1536) { 0.1f })
            val fragment1 = createTestFragment(text = "Best match")
            val fragment2 = createTestFragment(text = "Good match")
            val fragment3 = createTestFragment(text = "Okay match")

            val similarFragments = listOf(
                SimilarFragment(fragment1, 0.95),
                SimilarFragment(fragment2, 0.80),
                SimilarFragment(fragment3, 0.70)
            )

            val query = SimilarFragmentsQuery(userId = userId, queryText = queryText)

            coEvery { embeddingPort.embed(queryText) } returns queryEmbedding
            every { fragmentRepository.findSimilar(userId, queryEmbedding, 10) } returns similarFragments

            // When
            val results = useCase.findSimilar(query)

            // Then
            assertThat(results).hasSize(3)
            assertThat(results[0].similarity).isEqualTo(0.95)
            assertThat(results[1].similarity).isEqualTo(0.80)
            assertThat(results[2].similarity).isEqualTo(0.70)
        }
    }
}
