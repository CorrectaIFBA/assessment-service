package io.correcta.assessment.models.response

import io.correcta.assessment.entities.AssessmentSession
import io.correcta.assessment.entities.AssessmentSessionState
import java.time.Instant

/** Short form of a session, embedded in [GetAssessmentResponse]. */
data class GetAssessmentSessionSummaryResponse(
  val id: String,
  val state: AssessmentSessionState,
  val startedAt: String,
  val expiresAt: String?,
  val finishedAt: String?,
  val late: Boolean,
  val serverNow: String,
) {

  companion object {

    /** Maps a saved [AssessmentSession] at the server time [now]. */
    fun fromSession(session: AssessmentSession, now: Instant): GetAssessmentSessionSummaryResponse = GetAssessmentSessionSummaryResponse(
      id = session.id!!,
      state = session.state,
      startedAt = session.startedAt.toString(),
      expiresAt = session.expiresAt?.toString(),
      finishedAt = session.finishedAt?.toString(),
      late = session.isLate(now),
      serverNow = now.toString(),
    )
  }
}