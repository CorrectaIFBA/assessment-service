package io.correcta.assessment.models.response

import io.correcta.assessment.entities.AssessmentSession

data class GetAssessmentSessionResponse(
    val id: String,
    val assessmentId: String,
    val studentProfileId: String,
    val lastHeartbeatAt: String?,
    val finishedAt: String?,
    val createdAt: String,
    val updatedAt: String,
) {

    companion object {

        fun fromSession(session: AssessmentSession): GetAssessmentSessionResponse {
            return GetAssessmentSessionResponse(
                id = session.id!!,
                assessmentId = session.assessment.id!!,
                studentProfileId = session.studentProfileId,
                lastHeartbeatAt = session.lastHeartbeatAt?.toString(),
                finishedAt = session.finishedAt?.toString(),
                createdAt = session.createdAt!!.toString(),
                updatedAt = session.updatedAt!!.toString()
            )
        }
    }
}
