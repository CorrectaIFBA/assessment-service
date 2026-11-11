package io.correcta.assessment.entities

/** Visibility of a test case to students. Faculty always see everything. */
enum class AssessmentQuestionTestcaseType(
  val isListVisible: Boolean,
  val isInputVisible: Boolean,
  val isExpectedOutputVisible: Boolean,
) {
  PUBLIC(true, true, true),
  PRIVATE(true, false, false),
  EVALUATION(false, false, false),
}