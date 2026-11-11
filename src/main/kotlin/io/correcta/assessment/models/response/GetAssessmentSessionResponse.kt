package io.correcta.assessment.models.response

import io.correcta.assessment.entities.AssessmentSession
import io.correcta.assessment.entities.AssessmentSessionState
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/** Session of a student, with the rules the client needs. */
data class GetAssessmentSessionResponse(
  val id: String,
  val assessmentId: String,
  val assessmentTitle: String,
  val assessmentSlug: String,
  val cohortId: String?,
  val studentProfileId: String,
  val state: AssessmentSessionState,
  val startedAt: String,
  @field:Schema(description = "When the session stops accepting submissions. Null: it only ends by submission.")
  val expiresAt: String?,
  val finishedAt: String?,
  val lastHeartbeatAt: String?,
  @field:Schema(description = "Seconds the coding page was open.")
  val openSeconds: Int,
  @field:Schema(description = "Whether the session finished, or is still running, after the due date.")
  val late: Boolean,
  @field:Schema(description = "Server clock, so the client can correct its own.")
  val serverNow: String,
  val rules: Rules,
) {

  /** Assessment parameters that change what the student sees. */
  data class Rules(
    val canViewDefinitiveTestcaseScoringAfterSubmission: Boolean,
  )

  companion object {

    /** Maps a saved [AssessmentSession] at the server time [now]. */
    fun fromSession(
      session: AssessmentSession,
      now: Instant,
      canViewDefinitiveTestcaseScoringAfterSubmission: Boolean,
    ): GetAssessmentSessionResponse = GetAssessmentSessionResponse(
      id = session.id!!,
      assessmentId = session.assessment.id!!,
      assessmentTitle = session.assessment.title,
      assessmentSlug = session.assessment.slug,
      cohortId = session.assessment.cohortId,
      studentProfileId = session.studentProfileId,
      state = session.state,
      startedAt = session.startedAt.toString(),
      expiresAt = session.expiresAt?.toString(),
      finishedAt = session.finishedAt?.toString(),
      lastHeartbeatAt = session.lastHeartbeatAt?.toString(),
      openSeconds = session.openSeconds,
      late = session.isLate(now),
      serverNow = now.toString(),
      rules = Rules(
        canViewDefinitiveTestcaseScoringAfterSubmission = canViewDefinitiveTestcaseScoringAfterSubmission,
      ),
    )
  }
}

/** Whether the session finished, or is still running at [now], after the due date of its assessment. */
fun AssessmentSession.isLate(now: Instant): Boolean {
  val dueDate = assessment.dueDate ?: return false

  return (finishedAt ?: now) > dueDate
}