package io.correcta.assessment.models.response

import io.correcta.assessment.entities.ProgrammingLanguage

/** Harness and test cases of a question in one language, for other services. */
data class GetInternalAssessmentQuestionResponse(
  val id: String,
  val assessmentId: String,
  val language: ProgrammingLanguage,
  val harness: String,
  val testcases: List<GetAssessmentQuestionTestcaseResponse>,
)