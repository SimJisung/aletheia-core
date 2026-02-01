package com.aletheia.pros.api.dto.response

import com.aletheia.pros.domain.value.ValueImportance
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/**
 * Response containing value importance ratings.
 */
@Schema(description = "User's value importance ratings")
data class ValueImportanceResponse(
    @Schema(description = "Map of value axis to importance (1-10 scale)")
    val importance: Map<String, Double>,

    @Schema(description = "Version number of this configuration")
    val version: Int,

    @Schema(description = "Last update timestamp")
    val updatedAt: Instant
) {
    companion object {
        fun from(domain: ValueImportance): ValueImportanceResponse {
            // Convert all importances to 1-10 scale for display
            val displayImportance = domain.getAllImportances()
                .mapKeys { it.key.name }
                .mapValues { ValueImportance.denormalizeToScale(it.value) }

            return ValueImportanceResponse(
                importance = displayImportance,
                version = domain.version,
                updatedAt = domain.updatedAt
            )
        }
    }
}
