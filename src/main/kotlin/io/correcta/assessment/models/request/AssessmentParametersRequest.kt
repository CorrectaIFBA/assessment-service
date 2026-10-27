package io.correcta.assessment.models.request

import io.correcta.assessment.entities.Assessment
import io.correcta.assessment.entities.AssessmentParameters
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

data class AssessmentParametersRequest(
    @field:Schema(description = "Whether submissions are accepted after the due date.", defaultValue = "false")
    val canBeSubmittedAfterDueDate: Boolean = false,

    @field:Schema(description = "Whether the student must finish in one sitting.", defaultValue = "false")
    val needsToFinishInOneSitting: Boolean = false,

    @field:Schema(description = "Time limit in minutes. Null means no time limit.", example = "90")
    @field:Min(value = 1, message = "O tempo máximo deve ser de pelo menos 1 minuto")
    val maximumTimeAllowed: Int? = null,

    @field:Schema(description = "Whether page focus is monitored.", defaultValue = "false")
    val monitorForCheating: Boolean = false,

    @field:Schema(description = "Whether the student may view the definitive test case scoring after submission.", defaultValue = "true")
    val canViewDefinitiveTestcaseScoringAfterSubmission: Boolean = true,
) {
    fun toAssessmentParameters(assessment: Assessment): AssessmentParameters {
        return AssessmentParameters(
            assessment = assessment,
            canBeSubmittedAfterDueDate = this.canBeSubmittedAfterDueDate,
            needsToFinishInOneSitting = this.needsToFinishInOneSitting,
            maximumTimeAllowed = this.maximumTimeAllowed,
            monitorForCheating = this.monitorForCheating,
            canViewDefinitiveTestcaseScoringAfterSubmission = this.canViewDefinitiveTestcaseScoringAfterSubmission
        )
    }
}
