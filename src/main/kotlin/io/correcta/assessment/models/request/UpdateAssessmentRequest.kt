package io.correcta.assessment.models.request

import io.correcta.assessment.utils.SlugExt.SLUG_MAX_LENGTH
import io.correcta.assessment.utils.SlugExt.SLUG_PATTERN
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant

/** Body to replace the editable fields of an assessment. */
data class UpdateAssessmentRequest(
  @field:Schema(description = "Assessment title.", example = "Lista 1")
  @field:NotBlank(message = "O título é obrigatório")
  @field:Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
  val title: String,

  @field:Schema(
    description = "Address of the assessment in its cohort's URL, unique inside the cohort. Kept as is when omitted.",
    example = "lista-1",
  )
  @field:Size(max = SLUG_MAX_LENGTH, message = "O slug deve ter no máximo 160 caracteres")
  @field:Pattern(regexp = SLUG_PATTERN, message = "O slug deve conter apenas letras minúsculas, números e hífens")
  val slug: String? = null,

  @field:Schema(description = "Optional assessment description. Null clears it.")
  @field:Size(max = 1024, message = "A descrição deve ter no máximo 1024 caracteres")
  val description: String? = null,

  @field:Schema(description = "Optional due date (UTC ISO-8601). Null clears it.", example = "2026-11-30T23:59:00Z")
  val dueDate: Instant? = null,
)