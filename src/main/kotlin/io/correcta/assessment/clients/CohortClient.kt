package io.correcta.assessment.clients

import io.correcta.assessment.models.response.clients.CohortMembershipResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(
  name = "cohort-service",
  url = "\${clients.cohort-service.url}",
)
interface CohortClient {

  @GetMapping("/api/v1/internal/cohorts/{cohortId}/members/{profileId}")
  fun getCurrentMembership(
    @PathVariable cohortId: String,
    @PathVariable profileId: String,
  ): CohortMembershipResponse
}