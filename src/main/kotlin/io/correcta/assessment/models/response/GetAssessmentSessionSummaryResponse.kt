package io.correcta.assessment.models.response

import io.correcta.assessment.entities.AssessmentSession
import io.correcta.assessment.entities.AssessmentSessionState
import java.time.Instant



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

        fun fromSession(session: AssessmentSession, now: Instant): GetAssessmentSessionSummaryResponse {
            return GetAssessmentSessionSummaryResponse(
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
}
