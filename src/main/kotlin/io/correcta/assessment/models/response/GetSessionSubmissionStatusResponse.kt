package io.correcta.assessment.models.response

import io.correcta.assessment.entities.AssessmentSessionState

/** Whether a session still accepts submissions, for submission-service. */
data class GetSessionSubmissionStatusResponse(
  val sessionId: String,
  val state: AssessmentSessionState,
  val expiresAt: String?,
  val finishedAt: String?,
  val acceptsSubmissions: Boolean,
  val late: Boolean,
)