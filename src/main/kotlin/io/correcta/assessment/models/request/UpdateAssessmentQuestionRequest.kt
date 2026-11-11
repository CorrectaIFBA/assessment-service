package io.correcta.assessment.models.request

import io.correcta.assessment.utils.SlugExt.SLUG_MAX_LENGTH
import io.correcta.assessment.utils.SlugExt.SLUG_PATTERN
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal

data class UpdateAssessmentQuestionRequest(
  @field:Schema(description = "Zero-based question position inside the assessment.", example = "0")
  @field:NotNull(message = "A posição é obrigatória")
  @field:Min(value = 0, message = "A posição deve ser maior ou igual a 0")
  var position: Int?,

  @field:Schema(description = "Question title, shown in lists and headings.", example = "Soma de dois números")
  @field:NotBlank(message = "O título é obrigatório")
  @field:Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
  val title: String,

  @field:Schema(
    description = "Address of the question in its assessment's URL, unique inside the assessment. Kept as is when omitted.",
    example = "soma-de-dois-numeros",
  )
  @field:Size(max = SLUG_MAX_LENGTH, message = "O slug deve ter no máximo 160 caracteres")
  @field:Pattern(regexp = SLUG_PATTERN, message = "O slug deve conter apenas letras minúsculas, números e hífens")
  val slug: String? = null,

  @field:Schema(description = "Question statement in Markdown or HTML.", example = "Explique o conceito de recursão.")
  @field:NotBlank(message = "O enunciado é obrigatório")
  val statement: String,

  @field:Schema(description = "Languages the student may answer in, each with its starting code. At least one, each language once.")
  @field:NotEmpty(message = "Escolha ao menos uma linguagem")
  @field:Valid
  val codeDrafts: List<CodeDraftRequest> = emptyList(),

  @field:Schema(
    description = "Maximum grade of the question. The assessment grade is the sum of all questions.",
    example = "2.5",
  )
  @field:NotNull(message = "A nota máxima é obrigatória")
  @field:DecimalMin(value = "0", message = "A nota máxima deve ser maior ou igual a 0")
  @field:Digits(
    integer = 3,
    fraction = 2,
    message = "A nota máxima deve ter no máximo 3 dígitos inteiros e 2 decimais",
  )
  var maximumGrade: BigDecimal?,

  @field:Schema(description = "Whether students may copy and paste in this question.", defaultValue = "true")
  val allowCopyPasting: Boolean = true,
)