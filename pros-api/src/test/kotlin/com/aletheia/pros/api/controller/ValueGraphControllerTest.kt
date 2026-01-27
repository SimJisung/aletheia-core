package com.aletheia.pros.api.controller

import com.aletheia.pros.api.exception.GlobalExceptionHandler
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueEdgeId
import com.aletheia.pros.domain.common.ValueNodeId
import com.aletheia.pros.domain.value.EdgeType
import com.aletheia.pros.domain.value.Trend
import com.aletheia.pros.domain.value.ValueAxis
import com.aletheia.pros.domain.value.ValueEdge
import com.aletheia.pros.domain.value.ValueGraph
import com.aletheia.pros.domain.value.ValueGraphRepository
import com.aletheia.pros.domain.value.ValueNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
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

@ExtendWith(MockKExtension::class)
@DisplayName("ValueGraphController Tests")
class ValueGraphControllerTest {

    @MockK
    private lateinit var valueGraphRepository: ValueGraphRepository

    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper

    private val userId = UserId.generate()
    private val userIdHeader = userId.value.toString()

    @BeforeEach
    fun setUp() {
        val controller = ValueGraphController(
            valueGraphRepository = valueGraphRepository
        )
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
        objectMapper = ObjectMapper().findAndRegisterModules()
    }

    private fun createTestNodes(): List<ValueNode> {
        return ValueAxis.all().map { axis ->
            ValueNode(
                id = ValueNodeId.generate(),
                userId = userId,
                axis = axis,
                avgValence = when (axis) {
                    ValueAxis.GROWTH -> 0.7
                    ValueAxis.STABILITY -> -0.2
                    ValueAxis.FINANCIAL -> 0.3
                    else -> 0.0
                },
                recentTrend = when (axis) {
                    ValueAxis.GROWTH -> Trend.RISING
                    ValueAxis.STABILITY -> Trend.FALLING
                    else -> Trend.NEUTRAL
                },
                fragmentCount = if (axis == ValueAxis.GROWTH) 10.0 else 5.0,
                updatedAt = Instant.now()
            )
        }
    }

    private fun createTestEdges(): List<ValueEdge> {
        return listOf(
            ValueEdge(
                id = ValueEdgeId.generate(),
                userId = userId,
                fromAxis = ValueAxis.GROWTH,
                toAxis = ValueAxis.AUTONOMY,
                edgeType = EdgeType.SUPPORT,
                weight = 0.8,
                updatedAt = Instant.now()
            ),
            ValueEdge(
                id = ValueEdgeId.generate(),
                userId = userId,
                fromAxis = ValueAxis.STABILITY,
                toAxis = ValueAxis.GROWTH,
                edgeType = EdgeType.CONFLICT,
                weight = 0.6,
                updatedAt = Instant.now()
            )
        )
    }

    @Nested
    @DisplayName("GET /v1/values - Get Value Graph")
    inner class GetValueGraph {

        @Test
        fun `should return complete value graph`() {
            // Given
            val nodes = createTestNodes()
            val edges = createTestEdges()
            val graph = ValueGraph(userId, nodes, edges)

            every { valueGraphRepository.findValueGraph(userId) } returns graph

            // When/Then
            mockMvc.perform(
                get("/v1/values")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.nodes.length()").value(8))
                .andExpect(jsonPath("$.edges.length()").value(2))
                .andExpect(jsonPath("$.nodes[0].axis").exists())
                .andExpect(jsonPath("$.nodes[0].avgValence").exists())
                .andExpect(jsonPath("$.nodes[0].recentTrend").exists())
                .andExpect(jsonPath("$.edges[0].fromAxis").exists())
                .andExpect(jsonPath("$.edges[0].toAxis").exists())
                .andExpect(jsonPath("$.edges[0].edgeType").exists())
        }

        @Test
        fun `should initialize nodes for new user`() {
            // Given
            val nodes = createTestNodes()
            val graph = ValueGraph(userId, nodes, emptyList())

            every { valueGraphRepository.findValueGraph(userId) } returns null andThen graph
            every { valueGraphRepository.initializeNodesForUser(userId) } returns nodes

            // When/Then
            mockMvc.perform(
                get("/v1/values")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.nodes.length()").value(8))

            verify(exactly = 1) { valueGraphRepository.initializeNodesForUser(userId) }
        }

        @Test
        fun `should return 404 when graph initialization fails`() {
            // Given
            every { valueGraphRepository.findValueGraph(userId) } returns null
            every { valueGraphRepository.initializeNodesForUser(userId) } returns emptyList()

            // When/Then
            mockMvc.perform(
                get("/v1/values")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should return 400 when X-User-Id header is missing`() {
            // When/Then
            mockMvc.perform(
                get("/v1/values")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("Missing required header: X-User-Id"))
        }

        @Test
        fun `should return 400 when X-User-Id is invalid UUID`() {
            // When/Then
            mockMvc.perform(
                get("/v1/values")
                    .header("X-User-Id", "not-a-valid-uuid")
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("GET /v1/values/{axis} - Get Specific Value Axis")
    inner class GetValueAxis {

        @Test
        fun `should return specific value axis`() {
            // Given
            val node = ValueNode(
                id = ValueNodeId.generate(),
                userId = userId,
                axis = ValueAxis.GROWTH,
                avgValence = 0.7,
                recentTrend = Trend.RISING,
                fragmentCount = 15.0,
                updatedAt = Instant.now()
            )

            every { valueGraphRepository.findNodeByUserAndAxis(userId, ValueAxis.GROWTH) } returns node

            // When/Then
            mockMvc.perform(
                get("/v1/values/GROWTH")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.axis").value("GROWTH"))
                .andExpect(jsonPath("$.avgValence").value(0.7))
                .andExpect(jsonPath("$.recentTrend").value("RISING"))
                .andExpect(jsonPath("$.fragmentCount").value(15.0))
        }

        @Test
        fun `should handle lowercase axis name`() {
            // Given
            val node = ValueNode(
                id = ValueNodeId.generate(),
                userId = userId,
                axis = ValueAxis.STABILITY,
                avgValence = 0.3,
                recentTrend = Trend.NEUTRAL,
                fragmentCount = 5.0,
                updatedAt = Instant.now()
            )

            every { valueGraphRepository.findNodeByUserAndAxis(userId, ValueAxis.STABILITY) } returns node

            // When/Then
            mockMvc.perform(
                get("/v1/values/stability")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.axis").value("STABILITY"))
        }

        @Test
        fun `should return 404 when axis node not found`() {
            // Given
            every { valueGraphRepository.findNodeByUserAndAxis(userId, ValueAxis.HEALTH) } returns null

            // When/Then
            mockMvc.perform(
                get("/v1/values/HEALTH")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isNotFound)
        }

        @Test
        fun `should return 400 when axis name is invalid`() {
            // When/Then
            mockMvc.perform(
                get("/v1/values/INVALID_AXIS")
                    .header("X-User-Id", userIdHeader)
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `should return 400 when X-User-Id is missing`() {
            // When/Then
            mockMvc.perform(
                get("/v1/values/GROWTH")
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("GET /v1/values/axes - Get All Value Axis Definitions")
    inner class GetValueAxes {

        @Test
        fun `should return all 8 value axis definitions`() {
            // When/Then
            mockMvc.perform(
                get("/v1/values/axes")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].displayNameKo").exists())
                .andExpect(jsonPath("$[0].displayNameEn").exists())
                .andExpect(jsonPath("$[0].description").exists())
        }

        @Test
        fun `should include all expected axes`() {
            // When/Then
            mockMvc.perform(
                get("/v1/values/axes")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[?(@.name=='GROWTH')]").exists())
                .andExpect(jsonPath("$[?(@.name=='STABILITY')]").exists())
                .andExpect(jsonPath("$[?(@.name=='FINANCIAL')]").exists())
                .andExpect(jsonPath("$[?(@.name=='AUTONOMY')]").exists())
                .andExpect(jsonPath("$[?(@.name=='RELATIONSHIP')]").exists())
                .andExpect(jsonPath("$[?(@.name=='ACHIEVEMENT')]").exists())
                .andExpect(jsonPath("$[?(@.name=='HEALTH')]").exists())
                .andExpect(jsonPath("$[?(@.name=='MEANING')]").exists())
        }

        @Test
        fun `should not require authentication`() {
            // When/Then - no X-User-Id header needed
            mockMvc.perform(
                get("/v1/values/axes")
            )
                .andExpect(status().isOk)
        }
    }
}
