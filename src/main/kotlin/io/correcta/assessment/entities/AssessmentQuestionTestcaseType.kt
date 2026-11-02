package io.correcta.assessment.entities

enum class AssessmentQuestionTestcaseType(
    val isListVisible: Boolean,
    val isInputVisible: Boolean,
    val isExpectedOutputVisible: Boolean
) {
    PUBLIC(true, true, true),
    PRIVATE(true, false, false),
    EVALUATION(false, false, false)
}