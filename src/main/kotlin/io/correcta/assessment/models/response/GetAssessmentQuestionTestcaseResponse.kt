package io.correcta.assessment.models.response

import io.correcta.assessment.entities.AssessmentQuestionTestcase
import io.correcta.assessment.entities.AssessmentQuestionTestcaseType

data class GetAssessmentQuestionTestcaseResponse(
  val id: String,
  val questionId: String,
  val position: Int,
  val input: String?,
  val expectedOutput: String?,
  val timeLimitSeconds: Int,
  val memoryLimitMB: Int,
  val type: AssessmentQuestionTestcaseType,
  val createdAt: String,
  val updatedAt: String,
) {

  companion object {

    fun fromTestcase(
      testcase: AssessmentQuestionTestcase,
      isFaculty: Boolean,
    ): GetAssessmentQuestionTestcaseResponse = GetAssessmentQuestionTestcaseResponse(
      id = testcase.id!!,
      questionId = testcase.question.id!!,
      position = testcase.position,
      input = testcase.input.takeIf { isFaculty || testcase.type.isInputVisible },
      expectedOutput = testcase.expectedOutput.takeIf { isFaculty || testcase.type.isExpectedOutputVisible },
      timeLimitSeconds = testcase.timeLimitSeconds,
      memoryLimitMB = testcase.memoryLimitMB,
      type = testcase.type,
      createdAt = testcase.createdAt!!.toString(),
      updatedAt = testcase.updatedAt!!.toString(),
    )
  }
}