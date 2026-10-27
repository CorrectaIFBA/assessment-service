package io.correcta.assessment.models.response

import io.correcta.assessment.repositories.AssessmentQuestionRepository
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import io.correcta.assessment.entities.Assessment
import io.correcta.assessment.entities.AssessmentParameters
import io.correcta.assessment.entities.AssessmentState

data class GetAssessmentResponse(
    val id: String,
    val ownerProfileId: String,
    val cohortId: String?,
    val title: String,
    val description: String?,
    val state: AssessmentState,
    val dueDate: String?,
    val parameters: GetAssessmentParametersResponse?,
    val questionCount: Long,
    @field:Schema(description = "Sum of the maximum grades of the questions.")
    val totalGrade: BigDecimal,
    @field:Schema(description = "Final score of the authenticated student, once graded. Always null for faculty.")
    val score: BigDecimal?,
    val createdAt: String,
    val updatedAt: String,
) {

    companion object {

        fun fromAssessment(
            assessment: Assessment,
            parameters: AssessmentParameters? = null,
            questions: AssessmentQuestionRepository.QuestionSummary? = null,
            score: BigDecimal? = null,
        ): GetAssessmentResponse {
            return GetAssessmentResponse(
                id = assessment.id!!,
                ownerProfileId = assessment.ownerProfileId,
                cohortId = assessment.cohortId,
                title = assessment.title,
                description = assessment.description,
                state = assessment.state,
                dueDate = assessment.dueDate?.toString(),
                parameters = parameters?.let(GetAssessmentParametersResponse::fromParameters),
                questionCount = questions?.questionCount ?: 0,
                totalGrade = questions?.totalGrade ?: BigDecimal.ZERO,
                score = score,
                createdAt = assessment.createdAt!!.toString(),
                updatedAt = assessment.updatedAt!!.toString()
            )
        }
    }
}
