package io.correcta.assessment.models.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateAssessmentRequest(
    @field:Schema(description = "Assessment title.", example = "Lista 1")
    @field:NotBlank(message = "O título é obrigatório")
    @field:Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
    val title: String,

    @field:Schema(description = "Optional assessment description.")
    @field:Size(max = 1024, message = "A descrição deve ter no máximo 1024 caracteres")
    val description: String? = null,

    @field:Schema(description = "Cohort-service cohort ID. The authenticated profile must teach this cohort.")
    @field:NotBlank(message = "A turma é obrigatória")
    val cohortId: String?,

    @field:Schema(description = "Optional due date (UTC ISO-8601).", example = "2026-11-30T23:59:00Z")
    val dueDate: Instant? = null,

    @field:Schema(description = "Optional assessment parameters. Defaults apply when omitted.")
    @field:Valid
    val parameters: AssessmentParametersRequest? = null,
)
