package com.aletheia.pros.api.controller

import com.aletheia.pros.api.dto.response.ValueConflictResponse
import com.aletheia.pros.api.dto.response.ValueEdgeResponse
import com.aletheia.pros.api.dto.response.ValueGraphResponse
import com.aletheia.pros.api.dto.response.ValueNodeResponse
import com.aletheia.pros.api.dto.response.ValueSummaryResponse
import com.aletheia.pros.application.usecase.value.QueryValueGraphUseCase
import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.value.ValueAxis
import com.aletheia.pros.domain.value.ValueGraphRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * REST Controller for Value Graph operations.
 *
 * The Value Graph represents the user's value structure based on their
 * accumulated thought fragments. It is READ-ONLY from the API perspective.
 *
 * IMPORTANT: Conflicts in the value graph are NORMAL and preserved.
 * Humans have contradictory values, and the system respects this.
 */
@RestController
@RequestMapping("/v1/values")
@Tag(name = "Values", description = "User value graph (read-only)")
class ValueGraphController(
    private val valueGraphRepository: ValueGraphRepository,
    private val queryValueGraphUseCase: QueryValueGraphUseCase
) {

    /**
     * Gets the user's complete value graph.
     *
     * Returns all 8 value nodes and any edges between them.
     * Edges can be SUPPORT (values reinforce each other) or
     * CONFLICT (values are in tension).
     */
    @GetMapping
    @Operation(summary = "Get user's value graph")
    fun getValueGraph(
        @RequestHeader("X-User-Id") userId: String
    ): ResponseEntity<ValueGraphResponse> {
        val userIdObj = UserId(UUID.fromString(userId))

        var graph = valueGraphRepository.findValueGraph(userIdObj)
        if (graph == null) {
            // Ensure value graph nodes exist (idempotent under concurrency)
            valueGraphRepository.initializeNodesForUser(userIdObj)
            graph = valueGraphRepository.findValueGraph(userIdObj)
        }

        if (graph == null) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity.ok(ValueGraphResponse.from(graph))
    }

    /**
     * Gets a specific value axis for the user.
     */
    @GetMapping("/{axis}")
    @Operation(summary = "Get a specific value axis")
    fun getValueAxis(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable axis: String
    ): ResponseEntity<ValueNodeResponse> {
        val userIdObj = UserId(UUID.fromString(userId))

        val valueAxis = try {
            ValueAxis.valueOf(axis.uppercase())
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }

        val node = valueGraphRepository.findNodeByUserAndAxis(userIdObj, valueAxis)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(ValueNodeResponse.from(node))
    }

    /**
     * Gets the 8 value axes definitions.
     */
    @GetMapping("/axes")
    @Operation(summary = "Get all value axis definitions")
    fun getValueAxes(): ResponseEntity<List<ValueAxisDefinition>> {
        val axes = ValueAxis.all().map { axis ->
            ValueAxisDefinition(
                name = axis.name,
                displayNameKo = axis.displayNameKo,
                displayNameEn = axis.displayNameEn,
                description = axis.description
            )
        }

        return ResponseEntity.ok(axes)
    }

    /**
     * Gets all edges in the user's value graph.
     *
     * Edges can be SUPPORT (values reinforce each other) or
     * CONFLICT (values are in tension).
     */
    @GetMapping("/edges")
    @Operation(summary = "Get all edges in user's value graph")
    suspend fun getValueEdges(
        @RequestHeader("X-User-Id") userId: String
    ): ResponseEntity<List<ValueEdgeResponse>> {
        val userIdObj = UserId(UUID.fromString(userId))
        val edges = queryValueGraphUseCase.getEdges(userIdObj)
        return ResponseEntity.ok(edges.map { ValueEdgeResponse.from(it) })
    }

    /**
     * Gets value conflicts (tension between values).
     *
     * Note: Conflicts are NORMAL and expected. This is informational,
     * not a problem to be fixed. Humans naturally have competing values.
     */
    @GetMapping("/conflicts")
    @Operation(summary = "Get value conflicts (tension between values)")
    suspend fun getValueConflicts(
        @RequestHeader("X-User-Id") userId: String
    ): ResponseEntity<List<ValueConflictResponse>> {
        val userIdObj = UserId(UUID.fromString(userId))
        val conflicts = queryValueGraphUseCase.getConflicts(userIdObj)
        return ResponseEntity.ok(conflicts.map { ValueConflictResponse.from(it) })
    }

    /**
     * Gets a summary of the user's value profile.
     *
     * Returns top positive/negative values, dominant trend, and conflict count.
     */
    @GetMapping("/summary")
    @Operation(summary = "Get user's value profile summary")
    suspend fun getValueSummary(
        @RequestHeader("X-User-Id") userId: String
    ): ResponseEntity<ValueSummaryResponse> {
        val userIdObj = UserId(UUID.fromString(userId))
        val summary = queryValueGraphUseCase.getSummary(userIdObj)
        return ResponseEntity.ok(ValueSummaryResponse.from(summary))
    }
}

/**
 * Definition of a value axis.
 */
data class ValueAxisDefinition(
    val name: String,
    val displayNameKo: String,
    val displayNameEn: String,
    val description: String
)
