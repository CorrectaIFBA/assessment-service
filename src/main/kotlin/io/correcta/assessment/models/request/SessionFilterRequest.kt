package io.correcta.assessment.models.request

import io.correcta.assessment.entities.AssessmentSessionState

/** Filters of the session lists. */
data class SessionFilterRequest(
  val state: AssessmentSessionState? = null,
)