package io.correcta.assessment.models.request

import io.correcta.assessment.entities.AssessmentState
import io.swagger.v3.oas.annotations.media.Schema

data class AssessmentFilterRequest(
  @field:Schema(description = "Id of the cohort the assessment belongs to.")
  val cohortId: String? = null,

  @field:Schema(description = "Case-insensitive title fragment.")
  val title: String? = null,

  @field:Schema(description = "Assessment state. Only applies to faculty; students always see published assessments.")
  val state: AssessmentState? = null,
)