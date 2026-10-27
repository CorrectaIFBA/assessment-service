package io.correcta.assessment

import io.correcta.assessment.security.models.IdentityHeaders
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class HeaderAuthorizationTests(
    @Autowired val mockMvc: MockMvc
) {

    @Test
    fun `rejects requests without identity headers`() {
        mockMvc.get("/api/v1/assessments").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `scopes assessments to the profile from the identity headers`() {
        val assessmentId = mockMvc.post("/api/v1/assessments") {
            header(IdentityHeaders.PROFILE_ID, "profile-a")
            contentType = MediaType.APPLICATION_JSON
            content = """{"title": "Lista 1"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.ownerProfileId") { value("profile-a") }
        }.andReturn().response.contentAsString.let { Regex("\"id\":\"([^\"]+)\"").find(it)!!.groupValues[1] }

        mockMvc.get("/api/v1/assessments/$assessmentId") {
            header(IdentityHeaders.PROFILE_ID, "profile-a")
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/v1/assessments/$assessmentId") {
            header(IdentityHeaders.PROFILE_ID, "profile-b")
        }.andExpect { status { isNotFound() } }

        mockMvc.delete("/api/v1/assessments/$assessmentId") {
            header(IdentityHeaders.PROFILE_ID, "profile-b")
        }.andExpect { status { isNotFound() } }

        mockMvc.delete("/api/v1/assessments/$assessmentId") {
            header(IdentityHeaders.PROFILE_ID, "profile-a")
        }.andExpect { status { isNoContent() } }
    }
}
