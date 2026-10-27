package io.correcta.assessment.models.request

import io.correcta.assessment.entities.AssessmentParameters
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

data class ChangeAssessmentParametersRequest(
    @field:Schema(description = "Whether submissions are accepted after the due date. Null keeps the current value.")
    val canBeSubmittedAfterDueDate: Boolean? = null,

    @field:Schema(description = "Whether the student must finish in one sitting. Null keeps the current value.")
    val needsToFinishInOneSitting: Boolean? = null,

    @field:Schema(description = "Time limit in minutes. 0 removes the time limit. Null keeps the current value.", example = "90")
    @field:Min(value = 0, message = "O tempo máximo deve ser maior ou igual a 0")
    val maximumTimeAllowed: Int? = null,

    @field:Schema(description = "Whether page focus is monitored. Null keeps the current value.")
    val monitorForCheating: Boolean? = null,

    @field:Schema(description = "Whether the student may view the definitive test case scoring after submission. Null keeps the current value.")
    val canViewDefinitiveTestcaseScoringAfterSubmission: Boolean? = null,
) {
    fun applyChangesTo(assessmentParameters: AssessmentParameters) {
        canBeSubmittedAfterDueDate?.let { assessmentParameters.canBeSubmittedAfterDueDate = it }
        needsToFinishInOneSitting?.let { assessmentParameters.needsToFinishInOneSitting = it }
        maximumTimeAllowed?.let { assessmentParameters.maximumTimeAllowed = if (it == 0) null else it }
        monitorForCheating?.let { assessmentParameters.monitorForCheating = it }
        canViewDefinitiveTestcaseScoringAfterSubmission?.let { assessmentParameters.canViewDefinitiveTestcaseScoringAfterSubmission = it }
    }
}
