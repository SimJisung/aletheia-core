package com.aletheia.pros.domain.value

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueEdgeId
import com.aletheia.pros.domain.common.ValueNodeId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ValueGraph Domain Tests")
class ValueGraphTest {

    private val userId = UserId.generate()

    @Nested
    @DisplayName("ValueAxis")
    inner class ValueAxisTests {

        @Test
        fun `should have exactly 8 value axes`() {
            val axes = ValueAxis.all()
            assertThat(axes).hasSize(8)
        }

        @Test
        fun `should include all required axes`() {
            val axisNames = ValueAxis.all().map { it.name }

            assertThat(axisNames).containsExactlyInAnyOrder(
                "ACHIEVEMENT",
                "HEDONISM",
                "SECURITY",
                "SELF_DIRECTION",
                "BENEVOLENCE",
                "CONFORMITY",
                "POWER",
                "UNIVERSALISM"
            )
        }

        @Test
        fun `each axis should have display names in Korean and English`() {
            ValueAxis.all().forEach { axis ->
                assertThat(axis.displayNameKo).isNotBlank()
                assertThat(axis.displayNameEn).isNotBlank()
                assertThat(axis.description).isNotBlank()
            }
        }

        @Test
        fun `should find axis by name`() {
            val axis = ValueAxis.valueOf("ACHIEVEMENT")
            assertThat(axis).isEqualTo(ValueAxis.ACHIEVEMENT)
        }
    }

    @Nested
    @DisplayName("ValueNode")
    inner class ValueNodeTests {

        @Test
        fun `should create node with initial values`() {
            val node = ValueNode.createInitial(userId, ValueAxis.ACHIEVEMENT)

            assertThat(node.userId).isEqualTo(userId)
            assertThat(node.axis).isEqualTo(ValueAxis.ACHIEVEMENT)
            assertThat(node.avgValence).isEqualTo(0.0)
            assertThat(node.fragmentCount).isEqualTo(0)
        }

        @Test
        fun `should update valence with new fragment`() {
            val node = ValueNode.createInitial(userId, ValueAxis.HEDONISM)

            val updated = node.updateWithFragment(0.8)

            assertThat(updated.avgValence).isEqualTo(0.8)
            assertThat(updated.fragmentCount).isEqualTo(1)
        }

        @Test
        fun `should calculate running average correctly`() {
            var node = ValueNode.createInitial(userId, ValueAxis.SECURITY)

            // Add first fragment with valence 0.6
            node = node.updateWithFragment(0.6)
            assertThat(node.avgValence).isEqualTo(0.6)
            assertThat(node.fragmentCount).isEqualTo(1)

            // Add second fragment with valence 0.4
            // New avg = (0.6 * 1 + 0.4) / 2 = 0.5
            node = node.updateWithFragment(0.4)
            assertThat(node.avgValence).isEqualTo(0.5)
            assertThat(node.fragmentCount).isEqualTo(2)

            // Add third fragment with valence 0.8
            // New avg = (0.5 * 2 + 0.8) / 3 = 0.6
            node = node.updateWithFragment(0.8)
            assertThat(node.avgValence).isEqualTo(0.6, org.assertj.core.data.Offset.offset(0.001))
            assertThat(node.fragmentCount).isEqualTo(3)
        }

        @Test
        fun `should track trend direction`() {
            var node = ValueNode.createInitial(userId, ValueAxis.SELF_DIRECTION)

            // Start with negative, then improve
            node = node.updateWithFragment(-0.5)
            node = node.updateWithFragment(0.0)
            node = node.updateWithFragment(0.5)

            assertThat(node.trend).isEqualTo(ValueTrend.IMPROVING)
        }

        @Test
        fun `should clamp valence within bounds`() {
            var node = ValueNode.createInitial(userId, ValueAxis.BENEVOLENCE)

            // Extreme positive
            node = node.updateWithFragment(5.0) // Should clamp to 1.0
            assertThat(node.avgValence).isLessThanOrEqualTo(1.0)
        }
    }

    @Nested
    @DisplayName("ValueEdge")
    inner class ValueEdgeTests {

        @Test
        fun `should create support edge`() {
            val edge = ValueEdge.createSupport(
                userId = userId,
                fromAxis = ValueAxis.ACHIEVEMENT,
                toAxis = ValueAxis.POWER
            )

            assertThat(edge.edgeType).isEqualTo(EdgeType.SUPPORT)
            assertThat(edge.isSupport).isTrue()
            assertThat(edge.isConflict).isFalse()
        }

        @Test
        fun `should create conflict edge`() {
            val edge = ValueEdge.createConflict(
                userId = userId,
                fromAxis = ValueAxis.HEDONISM,
                toAxis = ValueAxis.CONFORMITY
            )

            assertThat(edge.edgeType).isEqualTo(EdgeType.CONFLICT)
            assertThat(edge.isConflict).isTrue()
            assertThat(edge.isSupport).isFalse()
        }

        @Test
        fun `should prevent self-referential edges`() {
            assertThatThrownBy {
                ValueEdge.createSupport(
                    userId = userId,
                    fromAxis = ValueAxis.SECURITY,
                    toAxis = ValueAxis.SECURITY
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("self")
        }

        @Test
        fun `should update edge weight`() {
            val edge = ValueEdge.createSupport(
                userId = userId,
                fromAxis = ValueAxis.BENEVOLENCE,
                toAxis = ValueAxis.UNIVERSALISM
            )

            val updated = edge.updateWeight(0.8)

            assertThat(updated.weight).isEqualTo(0.8)
            assertThat(updated.fromAxis).isEqualTo(edge.fromAxis)
            assertThat(updated.toAxis).isEqualTo(edge.toAxis)
        }

        @Test
        fun `should clamp weight within valid range`() {
            val edge = ValueEdge.createSupport(
                userId = userId,
                fromAxis = ValueAxis.ACHIEVEMENT,
                toAxis = ValueAxis.SELF_DIRECTION
            )

            val updated = edge.updateWeight(1.5)
            assertThat(updated.weight).isLessThanOrEqualTo(1.0)

            val updated2 = edge.updateWeight(-0.5)
            assertThat(updated2.weight).isGreaterThanOrEqualTo(0.0)
        }
    }

    @Nested
    @DisplayName("ValueGraph Aggregate")
    inner class ValueGraphAggregateTests {

        @Test
        fun `should contain all 8 nodes`() {
            val nodes = ValueAxis.all().map { axis ->
                ValueNode.createInitial(userId, axis)
            }
            val graph = ValueGraph(userId, nodes, emptyList())

            assertThat(graph.nodes).hasSize(8)
        }

        @Test
        fun `should get node by axis`() {
            val nodes = ValueAxis.all().map { axis ->
                ValueNode.createInitial(userId, axis)
            }
            val graph = ValueGraph(userId, nodes, emptyList())

            val achievementNode = graph.getNode(ValueAxis.ACHIEVEMENT)
            assertThat(achievementNode).isNotNull
            assertThat(achievementNode?.axis).isEqualTo(ValueAxis.ACHIEVEMENT)
        }

        @Test
        fun `should filter conflict edges`() {
            val nodes = listOf(
                ValueNode.createInitial(userId, ValueAxis.ACHIEVEMENT),
                ValueNode.createInitial(userId, ValueAxis.HEDONISM)
            )
            val edges = listOf(
                ValueEdge.createConflict(userId, ValueAxis.ACHIEVEMENT, ValueAxis.HEDONISM),
                ValueEdge.createSupport(userId, ValueAxis.ACHIEVEMENT, ValueAxis.POWER)
            )
            val graph = ValueGraph(userId, nodes, edges)

            assertThat(graph.conflicts).hasSize(1)
            assertThat(graph.conflicts[0].isConflict).isTrue()
        }

        @Test
        fun `should filter support edges`() {
            val nodes = listOf(
                ValueNode.createInitial(userId, ValueAxis.BENEVOLENCE),
                ValueNode.createInitial(userId, ValueAxis.UNIVERSALISM)
            )
            val edges = listOf(
                ValueEdge.createSupport(userId, ValueAxis.BENEVOLENCE, ValueAxis.UNIVERSALISM),
                ValueEdge.createConflict(userId, ValueAxis.ACHIEVEMENT, ValueAxis.HEDONISM)
            )
            val graph = ValueGraph(userId, nodes, edges)

            assertThat(graph.supports).hasSize(1)
            assertThat(graph.supports[0].isSupport).isTrue()
        }

        @Test
        fun `should get top positive values`() {
            val nodes = listOf(
                createNode(ValueAxis.ACHIEVEMENT, 0.8),
                createNode(ValueAxis.HEDONISM, 0.6),
                createNode(ValueAxis.SECURITY, -0.3),
                createNode(ValueAxis.POWER, 0.9)
            )
            val graph = ValueGraph(userId, nodes, emptyList())

            val topPositive = graph.topPositiveValues(2)

            assertThat(topPositive).hasSize(2)
            assertThat(topPositive[0].axis).isEqualTo(ValueAxis.POWER)
            assertThat(topPositive[1].axis).isEqualTo(ValueAxis.ACHIEVEMENT)
        }

        @Test
        fun `should get top negative values`() {
            val nodes = listOf(
                createNode(ValueAxis.CONFORMITY, -0.7),
                createNode(ValueAxis.SECURITY, -0.3),
                createNode(ValueAxis.HEDONISM, 0.5)
            )
            val graph = ValueGraph(userId, nodes, emptyList())

            val topNegative = graph.topNegativeValues(2)

            assertThat(topNegative).hasSize(2)
            assertThat(topNegative[0].axis).isEqualTo(ValueAxis.CONFORMITY)
        }
    }

    // Helper methods
    private fun createNode(axis: ValueAxis, valence: Double): ValueNode {
        return ValueNode(
            id = ValueNodeId.generate(),
            userId = userId,
            axis = axis,
            avgValence = valence,
            fragmentCount = 1,
            trend = ValueTrend.STABLE
        )
    }
}
