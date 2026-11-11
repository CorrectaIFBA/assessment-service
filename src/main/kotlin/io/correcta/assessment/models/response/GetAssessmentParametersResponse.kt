package io.correcta.assessment.models.response

import io.correcta.assessment.entities.AssessmentParameters

data class GetAssessmentParametersResponse(
  val id: String,
  val assessmentId: String,
  val canBeSubmittedAfterDueDate: Boolean,
  val maximumTimeAllowed: Int?,
  val showCompilationErrors: Boolean,
  val canViewDefinitiveTestcaseScoringAfterSubmission: Boolean,
  val createdAt: String,
  val updatedAt: String,
) {

  companion object {

    fun fromParameters(parameters: AssessmentParameters): GetAssessmentParametersResponse = GetAssessmentParametersResponse(
      id = parameters.id!!,
      assessmentId = parameters.assessment.id!!,
      canBeSubmittedAfterDueDate = parameters.canBeSubmittedAfterDueDate,
      maximumTimeAllowed = parameters.maximumTimeAllowed,
      showCompilationErrors = parameters.showCompilationErrors,
      canViewDefinitiveTestcaseScoringAfterSubmission = parameters.canViewDefinitiveTestcaseScoringAfterSubmission,
      createdAt = parameters.createdAt!!.toString(),
      updatedAt = parameters.updatedAt!!.toString(),
    )
  }
}