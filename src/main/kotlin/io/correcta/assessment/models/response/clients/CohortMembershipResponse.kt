package io.correcta.assessment.models.response.clients

data class CohortMembershipResponse(
    val cohortId: String,
    val cohortStatus: String,
    val profileId: String,
    val role: String
)
