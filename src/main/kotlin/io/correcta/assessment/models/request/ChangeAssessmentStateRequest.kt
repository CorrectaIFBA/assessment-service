package io.correcta.assessment.models.request

import io.correcta.assessment.entities.AssessmentState
import io.swagger.v3.oas.annotations.media.Schema

data class ChangeAssessmentStateRequest(
    @field:Schema(description = "Target state.", example = "PUBLISHED")
    val state: AssessmentState,
)
