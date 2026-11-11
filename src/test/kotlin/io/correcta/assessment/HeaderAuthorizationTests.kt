package io.correcta.assessment

import io.correcta.assessment.clients.CohortClient
import io.correcta.assessment.models.response.clients.CohortMembershipResponse
import io.correcta.assessment.security.models.IdentityHeaders
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class HeaderAuthorizationTests(
  @Autowired val mockMvc: MockMvc,
) {

  @MockitoBean
  lateinit var cohortClient: CohortClient

  @Test
  fun `rejects requests without identity headers`() {
    mockMvc.get("/api/v1/assessments").andExpect { status { isUnauthorized() } }
  }

  @Test
  fun `scopes assessments to the profile from the identity headers`() {
    // profile-a teaches cohort-1; profile-b is not a member, so the cohort service knows nothing of it.
    `when`(cohortClient.getCurrentMembership("cohort-1", "profile-a"))
      .thenReturn(CohortMembershipResponse("cohort-1", "ACTIVE", "profile-a", "FACULTY"))

    val assessmentId = mockMvc.post("/api/v1/assessments") {
      header(IdentityHeaders.PROFILE_ID, "profile-a")
      contentType = MediaType.APPLICATION_JSON
      content = """{"title": "Lista 1", "cohortId": "cohort-1"}"""
    }.andExpect {
      status { isCreated() }
      jsonPath("$.ownerProfileId") { value("profile-a") }
    }.andReturn().response.contentAsString.let { Regex("\"id\":\"([^\"]+)\"").find(it)!!.groupValues[1] }

    mockMvc.get("/api/v1/assessments/$assessmentId") {
      header(IdentityHeaders.PROFILE_ID, "profile-a")
    }.andExpect { status { isOk() } }

    mockMvc.get("/api/v1/assessments/$assessmentId") {
      header(IdentityHeaders.PROFILE_ID, "profile-b")
    }.andExpect { status { isForbidden() } }

    // Assessments are archived, not deleted.
    mockMvc.post("/api/v1/assessments/$assessmentId/archive") {
      header(IdentityHeaders.PROFILE_ID, "profile-b")
    }.andExpect { status { isForbidden() } }

    mockMvc.post("/api/v1/assessments/$assessmentId/archive") {
      header(IdentityHeaders.PROFILE_ID, "profile-a")
    }.andExpect { status { isOk() } }
  }
}