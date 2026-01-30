package com.aletheia.pros.api.controller

import com.aletheia.pros.api.dto.request.CreateFragmentRequest
import com.aletheia.pros.api.dto.response.FragmentListResponse
import com.aletheia.pros.api.dto.response.FragmentResponse
import com.aletheia.pros.api.dto.response.SimilarFragmentResponse
import com.aletheia.pros.api.security.SecurityUtils
import com.aletheia.pros.application.port.input.CreateFragmentCommand
import com.aletheia.pros.application.port.input.ListFragmentsQuery
import com.aletheia.pros.application.port.input.SimilarFragmentsQuery
import com.aletheia.pros.application.usecase.fragment.CreateFragmentUseCase
import com.aletheia.pros.application.usecase.fragment.DeleteFragmentUseCase
import com.aletheia.pros.application.usecase.fragment.QueryFragmentUseCase
import com.aletheia.pros.domain.common.FragmentId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * REST Controller for Thought Fragment operations.
 *
 * Fragments are the building blocks of the user's memory in PROS.
 * They are immutable (append-only) and can only be soft-deleted.
 */
@RestController
@RequestMapping("/v1/fragments")
@Tag(name = "Fragments", description = "Thought Fragment management")
class FragmentController(
    private val createFragmentUseCase: CreateFragmentUseCase,
    private val queryFragmentUseCase: QueryFragmentUseCase,
    private val deleteFragmentUseCase: DeleteFragmentUseCase
) {

    /**
     * Creates a new thought fragment.
     *
     * The fragment text is immutable once created.
     * Emotion analysis and embedding generation are performed automatically.
     */
    @PostMapping
    @Operation(summary = "Create a new thought fragment")
    suspend fun createFragment(
        @Valid @RequestBody request: CreateFragmentRequest
    ): ResponseEntity<FragmentResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val command = CreateFragmentCommand(
            userId = userId,
            text = request.text,
            topicHint = request.topicHint
        )

        val fragment = createFragmentUseCase.execute(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(FragmentResponse.from(fragment))
    }

    /**
     * Gets a fragment by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a fragment by ID")
    suspend fun getFragment(
        @PathVariable id: String
    ): ResponseEntity<FragmentResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val fragmentId = FragmentId(UUID.fromString(id))
        val fragment = queryFragmentUseCase.getById(fragmentId)
            ?: return ResponseEntity.notFound().build()
        if (fragment.userId != userId) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity.ok(FragmentResponse.from(fragment))
    }

    /**
     * Lists fragments for a user with pagination.
     */
    @GetMapping
    @Operation(summary = "List fragments with pagination")
    suspend fun listFragments(
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<FragmentListResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val query = ListFragmentsQuery(
            userId = userId,
            limit = limit.coerceIn(1, 100),
            offset = offset.coerceAtLeast(0)
        )

        val result = queryFragmentUseCase.list(query)
        val response = FragmentListResponse(
            fragments = result.fragments.map { FragmentResponse.from(it) },
            total = result.total,
            hasMore = result.hasMore
        )

        return ResponseEntity.ok(response)
    }

    /**
     * Soft-deletes a fragment.
     *
     * The fragment is not physically deleted, only marked as deleted.
     * This preserves the append-only nature of the fragment store.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a fragment")
    suspend fun deleteFragment(
        @PathVariable id: String
    ): ResponseEntity<Void> {
        val userId = SecurityUtils.getCurrentUserId()
        val fragmentId = FragmentId(UUID.fromString(id))
        val fragment = queryFragmentUseCase.getById(fragmentId)
            ?: return ResponseEntity.notFound().build()
        if (fragment.userId != userId) {
            return ResponseEntity.notFound().build()
        }
        val deleted = deleteFragmentUseCase.execute(fragmentId)

        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Finds similar fragments using semantic search.
     */
    @GetMapping("/similar")
    @Operation(summary = "Find similar fragments")
    suspend fun findSimilarFragments(
        @RequestParam queryText: String,
        @RequestParam(defaultValue = "10") topK: Int
    ): ResponseEntity<List<SimilarFragmentResponse>> {
        val userId = SecurityUtils.getCurrentUserId()
        val query = SimilarFragmentsQuery(
            userId = userId,
            queryText = queryText,
            topK = topK.coerceIn(1, 50)
        )

        val results = queryFragmentUseCase.findSimilar(query)
        val response = results.map { result ->
            SimilarFragmentResponse(
                fragment = FragmentResponse.from(result.fragment),
                similarity = result.similarity
            )
        }

        return ResponseEntity.ok(response)
    }
}
