package io.correcta.assessment.entities

/** Kinds of [AssessmentSessionEvent]. */
enum class AssessmentSessionEventType {
  STARTED,
  RESUMED,
  SUBMITTED,
  EXPIRED,
  CLOSED_BY_ARCHIVE,
  CLOSED_BY_FACULTY,
  REOPENED,
  EXTENDED,
}