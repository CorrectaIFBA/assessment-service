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
    val slug: String,
    val description: String?,
    val state: AssessmentState,
    val dueDate: String?,
    val parameters: GetAssessmentParametersResponse?,
    val questionCount: Long,
    @field:Schema(description = "Sum of the maximum grades of the questions.")
    val totalGrade: BigDecimal,
    @field:Schema(description = "Final score of the authenticated student, once graded. Always null for faculty.")
    val score: BigDecimal?,
    @field:Schema(description = "Session of the authenticated student, null when they have not started it. Always null for faculty.")
    val mySession: GetAssessmentSessionSummaryResponse?,
    val createdAt: String,
    val updatedAt: String,
) {

    companion object {

        fun fromAssessment(
            assessment: Assessment,
            parameters: AssessmentParameters? = null,
            questions: AssessmentQuestionRepository.QuestionSummary? = null,
            score: BigDecimal? = null,
            mySession: GetAssessmentSessionSummaryResponse? = null,
        ): GetAssessmentResponse {
            return GetAssessmentResponse(
                id = assessment.id!!,
                ownerProfileId = assessment.ownerProfileId,
                cohortId = assessment.cohortId,
                title = assessment.title,
                slug = assessment.slug,
                description = assessment.description,
                state = assessment.state,
                dueDate = assessment.dueDate?.toString(),
                parameters = parameters?.let(GetAssessmentParametersResponse::fromParameters),
                questionCount = questions?.questionCount ?: 0,
                totalGrade = questions?.totalGrade ?: BigDecimal.ZERO,
                score = score,
                mySession = mySession,
                createdAt = assessment.createdAt!!.toString(),
                updatedAt = assessment.updatedAt!!.toString()
            )
        }
    }
}
