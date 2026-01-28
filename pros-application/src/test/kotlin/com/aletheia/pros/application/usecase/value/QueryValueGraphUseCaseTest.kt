package com.aletheia.pros.application.usecase.value

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueEdgeId
import com.aletheia.pros.domain.common.ValueNodeId
import com.aletheia.pros.domain.fragment.FragmentRepository
import com.aletheia.pros.domain.value.EdgeType
import com.aletheia.pros.domain.value.Trend
import com.aletheia.pros.domain.value.ValueAxis
import com.aletheia.pros.domain.value.ValueEdge
import com.aletheia.pros.domain.value.ValueGraph
import com.aletheia.pros.domain.value.ValueGraphRepository
import com.aletheia.pros.domain.value.ValueNode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
@DisplayName("QueryValueGraphUseCase Tests")
class QueryValueGraphUseCaseTest {

    @MockK
    private lateinit var valueGraphRepository: ValueGraphRepository

    @MockK
    private lateinit var fragmentRepository: FragmentRepository

    private lateinit var useCase: QueryValueGraphUseCase

    private val userId = UserId.generate()

    @BeforeEach
    fun setUp() {
        useCase = QueryValueGraphUseCase(
            valueGraphRepository = valueGraphRepository,
            fragmentRepository = fragmentRepository
        )
    }

    @Nested
    @DisplayName("Get Edges")
    inner class GetEdges {

        @Test
        fun `should return all edges for user`() = runBlocking {
            // Given
            val edges = listOf(
                createEdge(ValueAxis.GROWTH, ValueAxis.STABILITY, EdgeType.CONFLICT, 0.5),
                createEdge(ValueAxis.FINANCIAL, ValueAxis.MEANING, EdgeType.SUPPORT, 0.3)
            )
            every { valueGraphRepository.findEdgesByUserId(userId) } returns edges

            // When
            val result = useCase.getEdges(userId)

            // Then
            assertThat(result).hasSize(2)
            assertThat(result[0].fromAxis).isEqualTo(ValueAxis.GROWTH)
            assertThat(result[0].toAxis).isEqualTo(ValueAxis.STABILITY)
        }
    }

    @Nested
    @DisplayName("Get Conflicts")
    inner class GetConflicts {

        @Test
        fun `should return only significant conflicts`() = runBlocking {
            // Given
            val conflictEdges = listOf(
                createEdge(ValueAxis.GROWTH, ValueAxis.STABILITY, EdgeType.CONFLICT, 0.7),
                createEdge(ValueAxis.AUTONOMY, ValueAxis.RELATIONSHIP, EdgeType.CONFLICT, 0.2)
            )
            every { valueGraphRepository.findConflictEdges(userId) } returns conflictEdges

            // When
            val result = useCase.getConflicts(userId)

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].axis1).isEqualTo(ValueAxis.GROWTH)
            assertThat(result[0].axis2).isEqualTo(ValueAxis.STABILITY)
            assertThat(result[0].strength).isEqualTo(0.7)
        }
    }

    @Nested
    @DisplayName("Get Summary")
    inner class GetSummary {

        @Test
        fun `should return value summary for user with data`() = runBlocking {
            // Given
            val nodes = ValueAxis.all().mapIndexed { index, axis ->
                createNode(axis, if (index < 3) 0.5 else -0.3)
            }
            val graph = ValueGraph(
                userId = userId,
                nodes = nodes,
                edges = listOf(
                    createEdge(ValueAxis.GROWTH, ValueAxis.STABILITY, EdgeType.CONFLICT, 0.5)
                )
            )

            every { valueGraphRepository.findValueGraph(userId) } returns graph
            every { fragmentRepository.countByUserId(userId) } returns 10L

            // When
            val result = useCase.getSummary(userId)

            // Then
            assertThat(result.userId).isEqualTo(userId)
            assertThat(result.totalFragments).isEqualTo(10)
            assertThat(result.topPositiveValues).isNotEmpty
            assertThat(result.topNegativeValues).isNotEmpty
            assertThat(result.conflictCount).isEqualTo(1)
        }

        @Test
        fun `should return empty summary when no data exists`() = runBlocking {
            // Given
            every { valueGraphRepository.findValueGraph(userId) } returns null

            // When
            val result = useCase.getSummary(userId)

            // Then
            assertThat(result.userId).isEqualTo(userId)
            assertThat(result.totalFragments).isEqualTo(0)
            assertThat(result.topPositiveValues).isEmpty()
            assertThat(result.topNegativeValues).isEmpty()
            assertThat(result.conflictCount).isEqualTo(0)
        }
    }

    private fun createNode(axis: ValueAxis, avgValence: Double): ValueNode {
        return ValueNode(
            id = ValueNodeId.generate(),
            userId = userId,
            axis = axis,
            avgValence = avgValence,
            recentTrend = if (avgValence > 0) Trend.RISING else Trend.FALLING,
            fragmentCount = 5.0,
            updatedAt = Instant.now()
        )
    }

    private fun createEdge(
        from: ValueAxis,
        to: ValueAxis,
        type: EdgeType,
        weight: Double
    ): ValueEdge {
        return ValueEdge(
            id = ValueEdgeId.generate(),
            userId = userId,
            fromAxis = from,
            toAxis = to,
            edgeType = type,
            weight = weight,
            updatedAt = Instant.now()
        )
    }
}
