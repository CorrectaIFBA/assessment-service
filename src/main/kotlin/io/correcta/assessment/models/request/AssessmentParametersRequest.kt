package io.correcta.assessment.models.request

import io.correcta.assessment.entities.Assessment
import io.correcta.assessment.entities.AssessmentParameters
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

/** Parameters given when an assessment is created. */
data class AssessmentParametersRequest(
  @field:Schema(description = "Whether submissions are accepted after the due date.", defaultValue = "false")
  val canBeSubmittedAfterDueDate: Boolean = false,

  @field:Schema(description = "Time limit in minutes. Null means no time limit.", example = "90")
  @field:Min(value = 1, message = "O tempo máximo deve ser de pelo menos 1 minuto")
  val maximumTimeAllowed: Int? = null,

  @field:Schema(
    description = "Whether the editor shows compilation errors while the student writes; off, only on submission.",
    defaultValue = "true",
  )
  val showCompilationErrors: Boolean = true,

  @field:Schema(
    description = "Whether the student may view the definitive test case scoring after submission.",
    defaultValue = "true",
  )
  val canViewDefinitiveTestcaseScoringAfterSubmission: Boolean = true,
) {
  /** Builds new, unsaved [AssessmentParameters] for [assessment]. */
  fun toAssessmentParameters(assessment: Assessment): AssessmentParameters = AssessmentParameters(
    assessment = assessment,
    canBeSubmittedAfterDueDate = this.canBeSubmittedAfterDueDate,
    maximumTimeAllowed = this.maximumTimeAllowed,
    showCompilationErrors = this.showCompilationErrors,
    canViewDefinitiveTestcaseScoringAfterSubmission = this.canViewDefinitiveTestcaseScoringAfterSubmission,
  )
}