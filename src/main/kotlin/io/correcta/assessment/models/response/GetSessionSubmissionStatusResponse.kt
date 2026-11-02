package io.correcta.assessment.models.response

import io.correcta.assessment.entities.AssessmentSessionState


data class GetSessionSubmissionStatusResponse(
    val sessionId: String,
    val state: AssessmentSessionState,
    val expiresAt: String?,
    val finishedAt: String?,
    val acceptsSubmissions: Boolean,
    val late: Boolean,
)
