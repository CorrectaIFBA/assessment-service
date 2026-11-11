package io.correcta.assessment.entities

/** Lifecycle of an [Assessment]: students only see published assessments. */
enum class AssessmentState {
  DRAFT,
  PUBLISHED,
  ARCHIVED,
}