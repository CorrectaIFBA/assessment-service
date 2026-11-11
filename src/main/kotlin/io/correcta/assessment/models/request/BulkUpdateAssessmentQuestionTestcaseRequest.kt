package io.correcta.assessment.models.request

import io.correcta.assessment.entities.AssessmentQuestionTestcaseType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

/** Changes applied to several test cases. Null fields keep their value. */
data class BulkUpdateAssessmentQuestionTestcaseRequest(
  @field:Schema(
    description = "ID's of the test cases to update, in the same order as the request list.",
    example = "[\"testcase1\", \"testcase2\"]",
  )
  @field:Size(min = 1, message = "A lista de IDs de casos de teste deve ter pelo menos 1 elemento")
  val testCaseIds: List<String>,

  @field:Schema(description = "Execution time limit in seconds.", defaultValue = "5")
  @field:Min(value = 1, message = "O limite de tempo deve ser de pelo menos 1 segundo")
  @field:Max(value = 60, message = "O limite de tempo deve ser de no máximo 60 segundos")
  val timeLimitSeconds: Int?,

  @field:Schema(description = "Memory limit in megabytes.", defaultValue = "256")
  @field:Min(value = 16, message = "O limite de memória deve ser de pelo menos 16 MB")
  @field:Max(value = 1024, message = "O limite de memória deve ser de no máximo 1024 MB")
  val memoryLimitMB: Int?,

  @field:Schema(
    description = "Visibility of the test case to students: PUBLIC shows everything, PRIVATE shows only that it exists, EVALUATION hides it.",
    example = "PUBLIC",
  )
  var type: AssessmentQuestionTestcaseType?,
)