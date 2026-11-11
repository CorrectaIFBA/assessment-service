package io.correcta.assessment.clients

import io.correcta.assessment.models.response.clients.CohortMembershipResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

/**
 * OpenFeign client for cohort-service internal endpoints, called inside the cluster without the gateway.
 */
@FeignClient(
  name = "cohort-service",
  url = "\${clients.cohort-service.url}",
)
interface CohortClient {

  /**
   * Returns the current membership of [profileId] in [cohortId].
   *
   * Throws `FeignException.NotFound` when the profile is not a current member of the cohort.
   */
  @GetMapping("/api/v1/internal/cohorts/{cohortId}/members/{profileId}")
  fun getCurrentMembership(
    @PathVariable cohortId: String,
    @PathVariable profileId: String,
  ): CohortMembershipResponse
}