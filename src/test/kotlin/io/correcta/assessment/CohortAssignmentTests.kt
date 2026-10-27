package io.correcta.assessment

import feign.FeignException
import feign.Request
import feign.Response
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
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CohortAssignmentTests(
    @Autowired val mockMvc: MockMvc
) {

    @MockitoBean
    lateinit var cohortClient: CohortClient

    @Test
    fun `assigns the assessment to a cohort where the caller is active faculty`() {
        `when`(cohortClient.getCurrentMembership("cohort-1", "faculty-a"))
            .thenReturn(CohortMembershipResponse("cohort-1", "ACTIVE", "faculty-a", "FACULTY"))

        createAssessment("faculty-a", "cohort-1").andExpect {
            status { isCreated() }
            jsonPath("$.cohortId") { value("cohort-1") }
        }
    }

    @Test
    fun `rejects students, non-members and inactive cohorts`() {
        `when`(cohortClient.getCurrentMembership("cohort-1", "student-a"))
            .thenReturn(CohortMembershipResponse("cohort-1", "ACTIVE", "student-a", "STUDENT"))
        `when`(cohortClient.getCurrentMembership("cohort-1", "outsider"))
            .thenThrow(feignError(404))
        `when`(cohortClient.getCurrentMembership("cohort-2", "faculty-a"))
            .thenReturn(CohortMembershipResponse("cohort-2", "ARCHIVED", "faculty-a", "FACULTY"))

        createAssessment("student-a", "cohort-1").andExpect { status { isForbidden() } }
        createAssessment("outsider", "cohort-1").andExpect { status { isForbidden() } }
        createAssessment("faculty-a", "cohort-2").andExpect { status { isConflict() } }
    }

    @Test
    fun `fails closed when cohort-service is unavailable`() {
        `when`(cohortClient.getCurrentMembership("cohort-1", "faculty-a")).thenThrow(feignError(500))

        createAssessment("faculty-a", "cohort-1").andExpect { status { isServiceUnavailable() } }
    }

    private fun createAssessment(profileId: String, cohortId: String): ResultActionsDsl {
        return mockMvc.post("/api/v1/assessments") {
            header(IdentityHeaders.PROFILE_ID, profileId)
            contentType = MediaType.APPLICATION_JSON
            content = """{"title": "Lista 1", "cohortId": "$cohortId"}"""
        }
    }

    private fun feignError(status: Int): FeignException {
        val request = Request.create(Request.HttpMethod.GET, "/", emptyMap(), null, Charsets.UTF_8, null)
        val response = Response.builder().status(status).request(request).headers(emptyMap()).build()

        return FeignException.errorStatus("CohortClient#getCurrentMembership", response)
    }
}
