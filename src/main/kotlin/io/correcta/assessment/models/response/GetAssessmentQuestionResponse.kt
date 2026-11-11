package io.correcta.assessment.models.response

import io.correcta.assessment.entities.AssessmentQuestion
import io.correcta.assessment.entities.ProgrammingLanguage
import java.math.BigDecimal

/** Question of an assessment. */
data class GetAssessmentQuestionResponse(
  val id: String,
  val assessmentId: String,
  val position: Int,
  val title: String,
  val slug: String,
  val statement: String,
  val codeDrafts: List<GetCodeDraftResponse>,
  val maximumGrade: BigDecimal,
  val allowCopyPasting: Boolean,
  val createdAt: String,
  val updatedAt: String,
) {

  /** Starting code of one language. [harness] is null for students. */
  data class GetCodeDraftResponse(
    val language: ProgrammingLanguage,
    val code: String,
    val harness: String?,
  )

  companion object {

    /**
     * Maps a saved [AssessmentQuestion]. Code drafts follow the [ProgrammingLanguage] order; harnesses are kept only for faculty.
     */
    fun fromQuestion(question: AssessmentQuestion, isFaculty: Boolean): GetAssessmentQuestionResponse = GetAssessmentQuestionResponse(
      id = question.id!!,
      assessmentId = question.assessment.id!!,
      position = question.position,
      title = question.title,
      slug = question.slug,
      statement = question.statement,
      codeDrafts = question.codeDrafts
        .sortedBy { it.language.ordinal }
        .map {
          GetCodeDraftResponse(
            language = it.language,
            code = it.code,
            harness = it.harness.takeIf { isFaculty },
          )
        },
      maximumGrade = question.maximumGrade,
      allowCopyPasting = question.allowCopyPasting,
      createdAt = question.createdAt!!.toString(),
      updatedAt = question.updatedAt!!.toString(),
    )
  }
}