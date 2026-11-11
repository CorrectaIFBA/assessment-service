package io.correcta.assessment.entities

/** State of an [AssessmentSession]. Every state except [IN_PROGRESS] is final. */
enum class AssessmentSessionState {
  IN_PROGRESS,
  SUBMITTED,
  EXPIRED,
  ;

  val isFinal: Boolean get() = this != IN_PROGRESS
}