package io.correcta.assessment.models.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import java.math.BigDecimal

data class UpdateAssessmentResultRequest(
    @field:Schema(description = "Final score. Null clears it.", example = "8.50")
    @field:DecimalMin(value = "0", message = "A nota final deve ser maior ou igual a 0")
    @field:Digits(
        integer = 3,
        fraction = 2,
        message = "A nota final deve ter no máximo 3 dígitos inteiros e 2 decimais"
    )
    val finalScore: BigDecimal? = null,

    @field:Schema(description = "General faculty comment. Null clears it.")
    val generalComment: String? = null,
)
