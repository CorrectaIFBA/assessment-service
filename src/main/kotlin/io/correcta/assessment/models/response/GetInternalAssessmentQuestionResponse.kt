package io.correcta.assessment.models.response

import io.correcta.assessment.entities.ProgrammingLanguage

data class GetInternalAssessmentQuestionResponse(
    val id: String,
    val assessmentId: String,
    val language: ProgrammingLanguage,
    val harness: String,
    val testcases: List<GetAssessmentQuestionTestcaseResponse>,
)
