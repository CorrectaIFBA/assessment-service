package io.correcta.assessment.models.response

import io.correcta.assessment.entities.AssessmentParameters

data class GetAssessmentParametersResponse(
    val id: String,
    val assessmentId: String,
    val canBeSubmittedAfterDueDate: Boolean,
    val needsToFinishInOneSitting: Boolean,
    val maximumTimeAllowed: Int?,
    val monitorForCheating: Boolean,
    val canViewDefinitiveTestcaseScoringAfterSubmission: Boolean,
    val createdAt: String,
    val updatedAt: String,
) {

    companion object {

        fun fromParameters(parameters: AssessmentParameters): GetAssessmentParametersResponse {
            return GetAssessmentParametersResponse(
                id = parameters.id!!,
                assessmentId = parameters.assessment.id!!,
                canBeSubmittedAfterDueDate = parameters.canBeSubmittedAfterDueDate,
                needsToFinishInOneSitting = parameters.needsToFinishInOneSitting,
                maximumTimeAllowed = parameters.maximumTimeAllowed,
                monitorForCheating = parameters.monitorForCheating,
                canViewDefinitiveTestcaseScoringAfterSubmission = parameters.canViewDefinitiveTestcaseScoringAfterSubmission,
                createdAt = parameters.createdAt!!.toString(),
                updatedAt = parameters.updatedAt!!.toString()
            )
        }
    }
}
