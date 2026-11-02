package io.correcta.assessment.models.request

import io.correcta.assessment.entities.AssessmentSessionState

data class SessionFilterRequest(
    val state: AssessmentSessionState? = null,
)
