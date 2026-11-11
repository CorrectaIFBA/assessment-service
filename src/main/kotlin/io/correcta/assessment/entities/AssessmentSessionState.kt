package io.correcta.assessment.entities

enum class AssessmentSessionState {
  IN_PROGRESS,
  SUBMITTED,
  EXPIRED,
  ;

  val isFinal: Boolean get() = this != IN_PROGRESS
}