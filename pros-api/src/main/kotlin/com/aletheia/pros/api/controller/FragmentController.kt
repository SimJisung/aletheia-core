package com.aletheia.pros.api.controller

import com.aletheia.pros.api.dto.request.CreateFragmentRequest
import com.aletheia.pros.api.dto.response.FragmentListResponse
import com.aletheia.pros.api.dto.response.FragmentResponse
import com.aletheia.pros.api.dto.response.SimilarFragmentResponse
import com.aletheia.pros.application.port.input.CreateFragmentCommand
import com.aletheia.pros.application.port.input.ListFragmentsQuery
import com.aletheia.pros.application.port.input.SimilarFragmentsQuery
import com.aletheia.pros.application.usecase.fragment.CreateFragmentUseCase
import com.aletheia.pros.application.usecase.fragment.DeleteFragmentUseCase
import com.aletheia.pros.application.usecase.fragment.QueryFragmentUseCase
import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.common.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
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
    fun createFragment(
        @RequestHeader("X-User-Id") userId: String,
        @Valid @RequestBody request: CreateFragmentRequest
    ): ResponseEntity<FragmentResponse> = runBlocking {
        val command = CreateFragmentCommand(
            userId = UserId(UUID.fromString(userId)),
            text = request.text,
            topicHint = request.topicHint
        )

        val fragment = createFragmentUseCase.execute(command)
        ResponseEntity.status(HttpStatus.CREATED).body(FragmentResponse.from(fragment))
    }

    /**
     * Gets a fragment by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a fragment by ID")
    fun getFragment(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable id: String
    ): ResponseEntity<FragmentResponse> = runBlocking {
        val userIdObj = UserId(UUID.fromString(userId))
        val fragmentId = FragmentId(UUID.fromString(id))
        val fragment = queryFragmentUseCase.getById(fragmentId)
            ?: return@runBlocking ResponseEntity.notFound().build()
        if (fragment.userId != userIdObj) {
            return@runBlocking ResponseEntity.notFound().build()
        }

        ResponseEntity.ok(FragmentResponse.from(fragment))
    }

    /**
     * Lists fragments for a user with pagination.
     */
    @GetMapping
    @Operation(summary = "List fragments with pagination")
    fun listFragments(
        @RequestHeader("X-User-Id") userId: String,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int
    ): ResponseEntity<FragmentListResponse> = runBlocking {
        val query = ListFragmentsQuery(
            userId = UserId(UUID.fromString(userId)),
            limit = limit.coerceIn(1, 100),
            offset = offset.coerceAtLeast(0)
        )

        val result = queryFragmentUseCase.list(query)
        val response = FragmentListResponse(
            fragments = result.fragments.map { FragmentResponse.from(it) },
            total = result.total,
            hasMore = result.hasMore
        )

        ResponseEntity.ok(response)
    }

    /**
     * Soft-deletes a fragment.
     *
     * The fragment is not physically deleted, only marked as deleted.
     * This preserves the append-only nature of the fragment store.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a fragment")
    fun deleteFragment(
        @RequestHeader("X-User-Id") userId: String,
        @PathVariable id: String
    ): ResponseEntity<Void> = runBlocking {
        val userIdObj = UserId(UUID.fromString(userId))
        val fragmentId = FragmentId(UUID.fromString(id))
        val fragment = queryFragmentUseCase.getById(fragmentId)
            ?: return@runBlocking ResponseEntity.notFound().build()
        if (fragment.userId != userIdObj) {
            return@runBlocking ResponseEntity.notFound().build()
        }
        val deleted = deleteFragmentUseCase.execute(fragmentId)

        if (deleted) {
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
    fun findSimilarFragments(
        @RequestHeader("X-User-Id") userId: String,
        @RequestParam queryText: String,
        @RequestParam(defaultValue = "10") topK: Int
    ): ResponseEntity<List<SimilarFragmentResponse>> = runBlocking {
        val query = SimilarFragmentsQuery(
            userId = UserId(UUID.fromString(userId)),
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

        ResponseEntity.ok(response)
    }
}
