package io.correcta.assessment.models.response

import io.correcta.assessment.entities.AssessmentQuestion
import io.correcta.assessment.entities.ProgrammingLanguage
import java.math.BigDecimal

data class GetAssessmentQuestionResponse(
    val id: String,
    val assessmentId: String,
    val position: Int,
    val statement: String,
    val codeDrafts: List<GetCodeDraftResponse>,
    val maximumGrade: BigDecimal,
    val allowCopyPasting: Boolean,
    val createdAt: String,
    val updatedAt: String,
) {

    data class GetCodeDraftResponse(
        val language: ProgrammingLanguage,
        val code: String,
    )

    companion object {

        fun fromQuestion(question: AssessmentQuestion): GetAssessmentQuestionResponse {
            return GetAssessmentQuestionResponse(
                id = question.id!!,
                assessmentId = question.assessment.id!!,
                position = question.position,
                statement = question.statement,
                // in the enum's order, so every client lists the languages the same way
                codeDrafts = question.codeDrafts
                    .sortedBy { it.language.ordinal }
                    .map { GetCodeDraftResponse(language = it.language, code = it.code) },
                maximumGrade = question.maximumGrade,
                allowCopyPasting = question.allowCopyPasting,
                createdAt = question.createdAt!!.toString(),
                updatedAt = question.updatedAt!!.toString()
            )
        }
    }
}
