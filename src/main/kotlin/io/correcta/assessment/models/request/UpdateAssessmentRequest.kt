package io.correcta.assessment.models.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class UpdateAssessmentRequest(
    @field:Schema(description = "Assessment title.", example = "Lista 1")
    @field:NotBlank(message = "O título é obrigatório")
    @field:Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
    val title: String,

    @field:Schema(description = "Optional assessment description. Null clears it.")
    @field:Size(max = 1024, message = "A descrição deve ter no máximo 1024 caracteres")
    val description: String? = null,

    @field:Schema(description = "Optional due date (UTC ISO-8601). Null clears it.", example = "2026-11-30T23:59:00Z")
    val dueDate: Instant? = null,
)
