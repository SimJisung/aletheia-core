package com.aletheia.pros.api.controller

import com.aletheia.pros.api.exception.GlobalExceptionHandler
import com.aletheia.pros.application.port.input.CreateFragmentCommand
import com.aletheia.pros.application.port.input.FragmentListResult
import com.aletheia.pros.application.port.input.ListFragmentsQuery
import com.aletheia.pros.application.port.input.SimilarFragmentResult
import com.aletheia.pros.application.port.input.SimilarFragmentsQuery
import com.aletheia.pros.application.usecase.fragment.CreateFragmentUseCase
import com.aletheia.pros.application.usecase.fragment.DeleteFragmentUseCase
import com.aletheia.pros.application.usecase.fragment.QueryFragmentUseCase
import com.aletheia.pros.domain.common.Embedding
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.fragment.Arousal
import com.aletheia.pros.domain.fragment.MoodValence
import com.aletheia.pros.domain.fragment.ThoughtFragment
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import java.util.UUID

@ExtendWith(MockKExtension::class)
@DisplayName("FragmentController Tests")
class FragmentControllerTest {

    @MockK
    private lateinit var createFragmentUseCase: CreateFragmentUseCase

    @MockK
    private lateinit var queryFragmentUseCase: QueryFragmentUseCase

    @MockK
    private lateinit var deleteFragmentUseCase: DeleteFragmentUseCase

    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper

    private val userId = UserId.generate()
    private val userIdHeader = userId.value.toString()
    private val testEmbedding = Embedding(FloatArray(1536) { 0.1f })

    @BeforeEach
    fun setUp() {
        val controller = FragmentController(
            createFragmentUseCase = createFragmentUseCase,
            queryFragmentUseCase = queryFragmentUseCase,
            deleteFragmentUseCase = deleteFragmentUseCase
        )
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
        objectMapper = ObjectMapper().findAndRegisterModules()
    }

    private fun createTestFragment(
        id: FragmentId = FragmentId.generate(),
        text: String = "Test fragment text",
        topicHint: String? = null
    ): ThoughtFragment {
        return ThoughtFragment.create(
            userId = userId,
            textRaw = text,
            moodValence = MoodValence(0.5),
            arousal = Arousal(0.5),
            topicHint = topicHint
        ).withEmbedding(testEmbedding).copy(id = id)
    }

    @Nested
    @DisplayName("POST /v1/fragments - Create Fragment")
    inner class CreateFragment {

        @Test
        fun `should create fragment successfully`() {
            // Given
            val requestBody = mapOf(
                "text" to "오늘 정말 기분 좋은 하루였다"
            )
            val createdFragment = createTestFragment(text = "오늘 정말 기분 좋은 하루였다")

            coEvery { createFragmentUseCase.execute(any()) } returns createdFragment

            // When/Then
            mockMvc.perform(
                post("/v1/fragments")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(createdFragment.id.toString()))
                .andExpect(jsonPath("$.text").value("오늘 정말 기분 좋은 하루였다"))
                .andExpect(jsonPath("$.moodValence").value(0.5))
                .andExpect(jsonPath("$.arousal").value(0.5))

            coVerify(exactly = 1) { createFragmentUseCase.execute(any()) }
        }

        @Test
        fun `should create fragment with topic hint`() {
            // Given
            val requestBody = mapOf(
                "text" to "Starting a new project",
                "topicHint" to "work"
            )
            val createdFragment = createTestFragment(
                text = "Starting a new project",
                topicHint = "work"
            )

            coEvery { createFragmentUseCase.execute(any()) } returns createdFragment

            // When/Then
            mockMvc.perform(
                post("/v1/fragments")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.topicHint").value("work"))
        }

        @Test
        fun `should return 400 when text is missing`() {
            // Given
            val requestBody = mapOf(
                "topicHint" to "work"
            )

            // When/Then
            mockMvc.perform(
                post("/v1/fragments")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `should return 400 when text is blank`() {
            // Given
            val requestBody = mapOf(
                "text" to "   "
            )

            // When/Then
            mockMvc.perform(
                post("/v1/fragments")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `should return 400 when X-User-Id header is missing`() {
            // Given
            val requestBody = mapOf("text" to "Test text")

            // When/Then
            mockMvc.perform(
                post("/v1/fragments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("Missing required header: X-User-Id"))
        }

        @Test
        fun `should return 400 when X-User-Id is invalid UUID`() {
            // Given
            val requestBody = mapOf("text" to "Test text")

            // When/Then
            mockMvc.perform(
                post("/v1/fragments")
                    .header("X-User-Id", "not-a-valid-uuid")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody))
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `should return 400 when JSON is malformed`() {
            // When/Then
            mockMvc.perform(
                post("/v1/fragments")
                    .header("X-User-Id", userIdHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
        }
    }

    @Nested
    @DisplayName("GET /v1/fragments/{id} - Get Fragment by ID")
    inner class GetFragment {

        @Test
        fun `should return fragment when found`() {
            // Given
            val fragmentId = FragmentId.generate()
            val fragment = createTestFragment(id = fragmentId)

            coEvery { queryFragmentUseCase.getById(fragmentId) } returns fragment

            // When/Then
            mockMvc.perform(
                get("/v1/fragments/${fragmentId.value}")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(fragmentId.toString()))
                .andExpect(jsonPath("$.text").value(fragment.textRaw))
        }

        @Test
        fun `should return 404 when fragment not found`() {
            // Given
            val fragmentId = FragmentId.generate()

            coEvery { queryFragmentUseCase.getById(fragmentId) } returns null

            // When/Then
            mockMvc.perform(
                get("/v1/fragments/${fragmentId.value}")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should return 404 when fragment belongs to different user`() {
            // Given
            val fragmentId = FragmentId.generate()
            val differentUserId = UserId.generate()
            val fragment = ThoughtFragment.create(
                userId = differentUserId,
                textRaw = "Someone else's fragment",
                moodValence = MoodValence(0.5),
                arousal = Arousal(0.5)
            ).withEmbedding(testEmbedding).copy(id = fragmentId)

            coEvery { queryFragmentUseCase.getById(fragmentId) } returns fragment

            // When/Then
            mockMvc.perform(
                get("/v1/fragments/${fragmentId.value}")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should return 400 when fragment ID is invalid UUID`() {
            // When/Then
            mockMvc.perform(
                get("/v1/fragments/invalid-uuid")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("GET /v1/fragments - List Fragments")
    inner class ListFragments {

        @Test
        fun `should return paginated fragments`() {
            // Given
            val fragments = listOf(
                createTestFragment(text = "Fragment 1"),
                createTestFragment(text = "Fragment 2")
            )
            val result = FragmentListResult(
                fragments = fragments,
                total = 10,
                hasMore = true
            )

            coEvery { queryFragmentUseCase.list(any()) } returns result

            // When/Then
            mockMvc.perform(
                get("/v1/fragments")
                    .header("X-User-Id", userIdHeader)
                    .param("limit", "20")
                    .param("offset", "0")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.fragments.length()").value(2))
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.hasMore").value(true))
        }

        @Test
        fun `should use default pagination values`() {
            // Given
            val result = FragmentListResult(
                fragments = emptyList(),
                total = 0,
                hasMore = false
            )

            coEvery { queryFragmentUseCase.list(any()) } returns result

            // When/Then
            mockMvc.perform(
                get("/v1/fragments")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isOk)

            coVerify {
                queryFragmentUseCase.list(match { query ->
                    query.limit == 20 && query.offset == 0
                })
            }
        }

        @Test
        fun `should coerce limit to valid range`() {
            // Given
            val result = FragmentListResult(
                fragments = emptyList(),
                total = 0,
                hasMore = false
            )

            coEvery { queryFragmentUseCase.list(any()) } returns result

            // When/Then - limit too high
            mockMvc.perform(
                get("/v1/fragments")
                    .header("X-User-Id", userIdHeader)
                    .param("limit", "200")
            )
                .andExpect(status().isOk)

            coVerify {
                queryFragmentUseCase.list(match { query ->
                    query.limit == 100  // coerced to max
                })
            }
        }

        @Test
        fun `should coerce negative offset to zero`() {
            // Given
            val result = FragmentListResult(
                fragments = emptyList(),
                total = 0,
                hasMore = false
            )

            coEvery { queryFragmentUseCase.list(any()) } returns result

            // When/Then
            mockMvc.perform(
                get("/v1/fragments")
                    .header("X-User-Id", userIdHeader)
                    .param("offset", "-5")
            )
                .andExpect(status().isOk)

            coVerify {
                queryFragmentUseCase.list(match { query ->
                    query.offset == 0  // coerced to min
                })
            }
        }
    }

    @Nested
    @DisplayName("DELETE /v1/fragments/{id} - Delete Fragment")
    inner class DeleteFragment {

        @Test
        fun `should delete fragment successfully`() {
            // Given
            val fragmentId = FragmentId.generate()
            val fragment = createTestFragment(id = fragmentId)

            coEvery { queryFragmentUseCase.getById(fragmentId) } returns fragment
            coEvery { deleteFragmentUseCase.execute(fragmentId) } returns true

            // When/Then
            mockMvc.perform(
                delete("/v1/fragments/${fragmentId.value}")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isNoContent)

            coVerify(exactly = 1) { deleteFragmentUseCase.execute(fragmentId) }
        }

        @Test
        fun `should return 404 when fragment not found`() {
            // Given
            val fragmentId = FragmentId.generate()

            coEvery { queryFragmentUseCase.getById(fragmentId) } returns null

            // When/Then
            mockMvc.perform(
                delete("/v1/fragments/${fragmentId.value}")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should return 404 when fragment belongs to different user`() {
            // Given
            val fragmentId = FragmentId.generate()
            val differentUserId = UserId.generate()
            val fragment = ThoughtFragment.create(
                userId = differentUserId,
                textRaw = "Someone else's fragment",
                moodValence = MoodValence(0.5),
                arousal = Arousal(0.5)
            ).withEmbedding(testEmbedding).copy(id = fragmentId)

            coEvery { queryFragmentUseCase.getById(fragmentId) } returns fragment

            // When/Then
            mockMvc.perform(
                delete("/v1/fragments/${fragmentId.value}")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should return 404 when delete returns false`() {
            // Given
            val fragmentId = FragmentId.generate()
            val fragment = createTestFragment(id = fragmentId)

            coEvery { queryFragmentUseCase.getById(fragmentId) } returns fragment
            coEvery { deleteFragmentUseCase.execute(fragmentId) } returns false

            // When/Then
            mockMvc.perform(
                delete("/v1/fragments/${fragmentId.value}")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    @DisplayName("GET /v1/fragments/similar - Find Similar Fragments")
    inner class FindSimilarFragments {

        @Test
        fun `should return similar fragments`() {
            // Given
            val fragment1 = createTestFragment(text = "Similar text 1")
            val fragment2 = createTestFragment(text = "Similar text 2")
            val results = listOf(
                SimilarFragmentResult(fragment1, 0.95),
                SimilarFragmentResult(fragment2, 0.85)
            )

            coEvery { queryFragmentUseCase.findSimilar(any()) } returns results

            // When/Then
            mockMvc.perform(
                get("/v1/fragments/similar")
                    .header("X-User-Id", userIdHeader)
                    .param("queryText", "Find similar content")
                    .param("topK", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].similarity").value(0.95))
                .andExpect(jsonPath("$[1].similarity").value(0.85))
        }

        @Test
        fun `should use default topK value`() {
            // Given
            coEvery { queryFragmentUseCase.findSimilar(any()) } returns emptyList()

            // When/Then
            mockMvc.perform(
                get("/v1/fragments/similar")
                    .header("X-User-Id", userIdHeader)
                    .param("queryText", "Search query")
            )
                .andExpect(status().isOk)

            coVerify {
                queryFragmentUseCase.findSimilar(match { query ->
                    query.topK == 10  // default value
                })
            }
        }

        @Test
        fun `should coerce topK to valid range`() {
            // Given
            coEvery { queryFragmentUseCase.findSimilar(any()) } returns emptyList()

            // When/Then - topK too high
            mockMvc.perform(
                get("/v1/fragments/similar")
                    .header("X-User-Id", userIdHeader)
                    .param("queryText", "Search query")
                    .param("topK", "100")
            )
                .andExpect(status().isOk)

            coVerify {
                queryFragmentUseCase.findSimilar(match { query ->
                    query.topK == 50  // coerced to max
                })
            }
        }

        @Test
        fun `should return 400 when queryText is missing`() {
            // When/Then
            mockMvc.perform(
                get("/v1/fragments/similar")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isBadRequest)
        }
    }
}
