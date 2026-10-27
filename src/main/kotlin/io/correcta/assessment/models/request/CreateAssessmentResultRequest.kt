package io.correcta.assessment.models.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class CreateAssessmentResultRequest(
    @field:Schema(description = "Identity-service profile ID of the graded student.")
    @field:NotBlank(message = "O perfil do aluno é obrigatório")
    val studentProfileId: String,

    @field:Schema(description = "Optional final score.", example = "8.50")
    @field:DecimalMin(value = "0", message = "A nota final deve ser maior ou igual a 0")
    @field:Digits(integer = 3, fraction = 2, message = "A nota final deve ter no máximo 3 dígitos inteiros e 2 decimais")
    val finalScore: BigDecimal? = null,

    @field:Schema(description = "Optional general faculty comment.")
    val generalComment: String? = null,
)
