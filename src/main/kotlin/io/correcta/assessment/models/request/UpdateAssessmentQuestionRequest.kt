package io.correcta.assessment.models.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class UpdateAssessmentQuestionRequest(
    @field:Schema(description = "Zero-based question position inside the assessment.", example = "0")
    @field:NotNull(message = "A posição é obrigatória")
    @field:Min(value = 0, message = "A posição deve ser maior ou igual a 0")
    val position: Int?,

    @field:Schema(description = "Question statement in Markdown or HTML.", example = "Explique o conceito de recursão.")
    @field:NotBlank(message = "O enunciado é obrigatório")
    val statement: String,

    @field:Schema(description = "Languages the student may answer in, each with its starting code. At least one, each language once.")
    @field:NotEmpty(message = "Escolha ao menos uma linguagem")
    @field:Valid
    val codeDrafts: List<CodeDraftRequest> = emptyList(),

    @field:Schema(description = "Maximum grade of the question. The assessment grade is the sum of all questions.", example = "2.5")
    @field:NotNull(message = "A nota máxima é obrigatória")
    @field:DecimalMin(value = "0", message = "A nota máxima deve ser maior ou igual a 0")
    @field:Digits(integer = 3, fraction = 2, message = "A nota máxima deve ter no máximo 3 dígitos inteiros e 2 decimais")
    var maximumGrade: BigDecimal?,

    @field:Schema(description = "Whether students may copy and paste in this question.", defaultValue = "true")
    val allowCopyPasting: Boolean = true,
)
