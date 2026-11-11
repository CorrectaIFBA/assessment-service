package io.correcta.assessment.models.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class SessionTimeChangeRequest(
  @field:Schema(description = "Minutes added to the session.", example = "15")
  @field:NotNull(message = "Os minutos extras são obrigatórios")
  @field:Min(value = 1, message = "Conceda pelo menos 1 minuto")
  @field:Max(value = 10080, message = "Conceda no máximo 10080 minutos (7 dias)")
  var extraMinutes: Int? = null,

  @field:Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres")
  val reason: String? = null,
)