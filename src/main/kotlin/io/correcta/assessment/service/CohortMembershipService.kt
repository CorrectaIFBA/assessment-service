package io.correcta.assessment.service

import feign.FeignException
import io.correcta.assessment.clients.CohortClient
import io.correcta.assessment.exceptions.ServiceUnavailableException
import io.correcta.assessment.models.response.clients.CohortMembershipResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/** Reads cohort memberships from cohort-service. */
@Service
class CohortMembershipService(
  val cohortClient: CohortClient,
) {

  private val log: Logger = LoggerFactory.getLogger(CohortMembershipService::class.java)

  /**
   * Returns the current membership, or null when the profile is not a member.
   *
   * Other cohort-service failures throw [ServiceUnavailableException], so access fails closed.
   */
  fun findCurrentMembership(cohortId: String, profileId: String): CohortMembershipResponse? = try {
    cohortClient.getCurrentMembership(cohortId, profileId)
  } catch (exception: FeignException.NotFound) {
    null
  } catch (exception: FeignException) {
    log.error("Cohort-service request failed with status {}", exception.status(), exception)

    throw ServiceUnavailableException("O serviço de turmas está indisponível no momento. Tente novamente mais tarde.")
  }
}