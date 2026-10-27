package io.correcta.assessment.models.response

import io.correcta.assessment.entities.AssessmentResult
import java.math.BigDecimal

data class GetAssessmentResultResponse(
    val id: String,
    val assessmentId: String,
    val studentProfileId: String,
    val finalScore: BigDecimal?,
    val generalComment: String?,
    val gradedByProfileId: String?,
    val gradedAt: String?,
    val createdAt: String,
    val updatedAt: String,
) {

    companion object {

        fun fromResult(result: AssessmentResult): GetAssessmentResultResponse {
            return GetAssessmentResultResponse(
                id = result.id!!,
                assessmentId = result.assessment.id!!,
                studentProfileId = result.studentProfileId,
                finalScore = result.finalScore,
                generalComment = result.generalComment,
                gradedByProfileId = result.gradedByProfileId,
                gradedAt = result.gradedAt?.toString(),
                createdAt = result.createdAt!!.toString(),
                updatedAt = result.updatedAt!!.toString()
            )
        }
    }
}
