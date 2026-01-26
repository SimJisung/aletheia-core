package com.aletheia.pros.application.usecase.fragment

import com.aletheia.pros.domain.common.FragmentId
import com.aletheia.pros.domain.fragment.FragmentRepository

/**
 * Use case for soft-deleting a thought fragment.
 *
 * Design Principle:
 * - Fragments are NEVER physically deleted
 * - Soft-delete sets the deletedAt timestamp
 * - Deleted fragments are excluded from queries but data is preserved
 */
class DeleteFragmentUseCase(
    private val fragmentRepository: FragmentRepository
) {

    /**
     * Soft-deletes a fragment.
     *
     * @param fragmentId The fragment ID to delete
     * @return True if deleted, false if not found or already deleted
     */
    suspend fun execute(fragmentId: FragmentId): Boolean {
        return fragmentRepository.softDelete(fragmentId)
    }
}
