package io.correcta.assessment.service

import feign.FeignException
import feign.Request
import feign.Response
import io.correcta.assessment.clients.CohortClient
import io.correcta.assessment.entities.AssessmentSessionState
import io.correcta.assessment.exceptions.ServiceUnavailableException
import io.correcta.assessment.repositories.AssessmentSessionRepository
import io.correcta.assessment.support.Fixtures.assessment
import io.correcta.assessment.support.Fixtures.membership
import io.correcta.assessment.support.Fixtures.session
import io.correcta.assessment.support.mock
import io.correcta.assessment.support.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify

class SupportServicesTest {

  @Test
  fun `the sweeper refreshes every running session, even after a failure`() {
    val repository = mock<AssessmentSessionRepository>()
    val sessionService = mock<AssessmentSessionService>()
    val sweeper = AssessmentSessionSweeper(repository, sessionService)
    val assessment = assessment()
    whenever(repository.findByState(AssessmentSessionState.IN_PROGRESS))
      .thenReturn(listOf(session(assessment, id = "session-1"), session(assessment, id = "session-2")))
    doThrow(IllegalStateException("boom")).`when`(sessionService).refreshSession("session-1")

    sweeper.sweep()

    verify(sessionService).refreshSession("session-2")
    assertSame(repository, sweeper.assessmentSessionRepository)
    assertSame(sessionService, sweeper.assessmentSessionService)
  }

  @Test
  fun `memberships come from cohort-service and its failures fail closed`() {
    val client = mock<CohortClient>()
    val service = CohortMembershipService(client)
    whenever(client.getCurrentMembership("cohort-1", "faculty-a")).thenReturn(membership())
    whenever(client.getCurrentMembership("cohort-1", "outsider")).thenThrow(feignError(404))
    whenever(client.getCurrentMembership("cohort-1", "anyone")).thenThrow(feignError(500))

    assertEquals("FACULTY", service.findCurrentMembership("cohort-1", "faculty-a")!!.role)
    assertNull(service.findCurrentMembership("cohort-1", "outsider"))
    assertThrows<ServiceUnavailableException> { service.findCurrentMembership("cohort-1", "anyone") }
    assertSame(client, service.cohortClient)
  }

  private fun feignError(status: Int): FeignException {
    val request = Request.create(Request.HttpMethod.GET, "/", emptyMap(), null, Charsets.UTF_8, null)
    val response = Response.builder().status(status).request(request).headers(emptyMap()).build()

    return FeignException.errorStatus("CohortClient#getCurrentMembership", response)
  }
}