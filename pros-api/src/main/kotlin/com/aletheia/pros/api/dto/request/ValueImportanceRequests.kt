package com.aletheia.pros.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

/**
 * Request for setting value importance ratings.
 */
@Schema(description = "Request to set value importance ratings")
data class SetValueImportanceRequest(
    @Schema(
        description = "Map of value axis name to importance score (1-10). Valid axis names: GROWTH, STABILITY, FINANCIAL, AUTONOMY, RELATIONSHIP, ACHIEVEMENT, HEALTH, MEANING",
        example = """{"GROWTH": 9, "STABILITY": 4, "AUTONOMY": 8}"""
    )
    @field:Size(min = 1, max = 8, message = "Must provide 1-8 importance values")
    val importance: Map<String, Double>
) {
    init {
        importance.values.forEach { value ->
            require(value in 1.0..10.0) {
                "Importance values must be between 1 and 10"
            }
        }
    }
}
