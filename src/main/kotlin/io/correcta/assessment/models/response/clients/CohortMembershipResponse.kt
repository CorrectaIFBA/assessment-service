package io.correcta.assessment.models.response.clients

/** Current membership of a profile in a cohort, as returned by cohort-service. */
data class CohortMembershipResponse(
  val cohortId: String,
  val cohortStatus: String,
  val profileId: String,
  val role: String,
)