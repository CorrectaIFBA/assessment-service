package io.correcta.assessment.models.request

import io.correcta.assessment.entities.CodeDraft
import io.correcta.assessment.entities.ProgrammingLanguage
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CodeDraftRequest(
    @field:Schema(description = "Language the student may answer in.", example = "PYTHON")
    @field:NotNull(message = "A linguagem do código é obrigatória")
    val language: ProgrammingLanguage?,

    @field:Schema(description = "Starting code handed to the student in this language. Kept as sent, whitespace included.", defaultValue = "")
    @field:Size(max = 65535, message = "O código inicial deve ter no máximo 65535 caracteres")
    val code: String = "",
) {
    // non-null language is guaranteed by @NotNull; nullable only so a missing field fails validation instead of deserialization
    fun toCodeDraft(): CodeDraft = CodeDraft(language = language!!, code = code)
}
