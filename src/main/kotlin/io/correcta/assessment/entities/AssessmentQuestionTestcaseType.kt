package io.correcta.assessment.entities

enum class AssessmentQuestionTestcaseType(
    val isListVisible: Boolean,
    val isInputVisible: Boolean,
    val isExpectedOutputVisible: Boolean
) {
    PUBLIC(true, true, true), // for general purpose test cases that are visible to students
    PRIVATE(true, false, false), // for edge cases whose values are not visible to students and are used for evaluation
    EVALUATION(false, false, false) // for test cases that are not visible to students and are used for evaluation
}