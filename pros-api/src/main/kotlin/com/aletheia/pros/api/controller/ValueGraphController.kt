package com.aletheia.pros.api.controller

import com.aletheia.pros.api.dto.response.ValueGraphResponse
import com.aletheia.pros.api.dto.response.ValueNodeResponse
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
    private val valueGraphRepository: ValueGraphRepository
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

        // Initialize value graph if it doesn't exist
        if (!valueGraphRepository.hasValueGraph(userIdObj)) {
            valueGraphRepository.initializeNodesForUser(userIdObj)
        }

        val graph = valueGraphRepository.findValueGraph(userIdObj)
            ?: return ResponseEntity.notFound().build()

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
