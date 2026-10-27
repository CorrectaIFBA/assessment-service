package io.correcta.assessment

import io.correcta.assessment.clients.CohortClient
import io.correcta.assessment.models.response.clients.CohortMembershipResponse
import io.correcta.assessment.security.models.IdentityHeaders
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AssessmentQuestionTestcaseTests(
    @Autowired val mockMvc: MockMvc,
) {

    @MockitoBean
    lateinit var cohortClient: CohortClient

    @BeforeEach
    fun mockMemberships() {
        `when`(cohortClient.getCurrentMembership("cohort-1", "faculty-a"))
            .thenReturn(CohortMembershipResponse("cohort-1", "ACTIVE", "faculty-a", "FACULTY"))
    }

    @Test
    fun `test cases are appended after the last one`() {
        val (assessmentId, questionId) = createQuestion()

        createTestcase(assessmentId, questionId, "1").andExpect {
            status { isCreated() }
            jsonPath("$.position") { value(1) }
        }
        createTestcase(assessmentId, questionId, "2").andExpect { jsonPath("$.position") { value(2) } }
        createTestcase(assessmentId, questionId, "3", position = 2).andExpect { status { isConflict() } }
    }

    @Test
    fun `reordering swaps positions without colliding`() {
        val (assessmentId, questionId) = createQuestion()
        val first = idOf(createTestcase(assessmentId, questionId, "1"))
        val second = idOf(createTestcase(assessmentId, questionId, "2"))
        val third = idOf(createTestcase(assessmentId, questionId, "3"))

        post("/api/v1/assessments/$assessmentId/questions/$questionId/testcases/reorder", """["$third", "$first", "$second"]""")
            .andExpect { status { isOk() } }

        getTestcases(assessmentId, questionId).andExpect {
            jsonPath("$[0].id") { value(third) }
            jsonPath("$[1].id") { value(first) }
            jsonPath("$[2].id") { value(second) }
        }

        // the new order must name every test case of the question
        post("/api/v1/assessments/$assessmentId/questions/$questionId/testcases/reorder", """["$third", "$first"]""")
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `bulk edit changes only the fields sent`() {
        val (assessmentId, questionId) = createQuestion()
        val first = idOf(createTestcase(assessmentId, questionId, "1"))
        val second = idOf(createTestcase(assessmentId, questionId, "2"))

        post(
            "/api/v1/assessments/$assessmentId/questions/$questionId/testcases/bulk-edit",
            """{"testCaseIds": ["$first", "$second"], "type": "EVALUATION"}"""
        ).andExpect {
            status { isOk() }
            jsonPath("$[0].type") { value("EVALUATION") }
            jsonPath("$[1].type") { value("EVALUATION") }
            jsonPath("$[0].timeLimitSeconds") { value(5) }
        }

        post(
            "/api/v1/assessments/$assessmentId/questions/$questionId/testcases/bulk-edit",
            """{"testCaseIds": ["$first", "missing"], "timeLimitSeconds": 2}"""
        ).andExpect { status { isNotFound() } }
    }

    @Test
    fun `bulk delete removes only the chosen test cases`() {
        val (assessmentId, questionId) = createQuestion()
        val first = idOf(createTestcase(assessmentId, questionId, "1"))
        val second = idOf(createTestcase(assessmentId, questionId, "2"))
        val third = idOf(createTestcase(assessmentId, questionId, "3"))

        post("/api/v1/assessments/$assessmentId/questions/$questionId/testcases/bulk-delete", """["$first", "$third"]""")
            .andExpect { status { isNoContent() } }

        getTestcases(assessmentId, questionId).andExpect {
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].id") { value(second) }
        }

        post("/api/v1/assessments/$assessmentId/questions/$questionId/testcases/bulk-delete", """["$first"]""")
            .andExpect { status { isNotFound() } }
    }

    private fun createQuestion(): Pair<String, String> {
        val assessmentId = idOf(post("/api/v1/assessments", """{"title": "Lista 1", "cohortId": "cohort-1"}"""))
        val questionId = idOf(
            post(
                "/api/v1/assessments/$assessmentId/questions",
                """{"statement": "Some dois números.", "codeDrafts": [{"language": "C", "code": ""}], "maximumGrade": 2}"""
            )
        )
        return assessmentId to questionId
    }

    private fun createTestcase(assessmentId: String, questionId: String, output: String, position: Int? = null): ResultActionsDsl {
        val positionField = position?.let { """"position": $it,""" } ?: ""

        return post(
            "/api/v1/assessments/$assessmentId/questions/$questionId/testcases",
            """{$positionField "input": "1 2", "expectedOutput": "$output", "type": "PUBLIC"}"""
        )
    }

    private fun getTestcases(assessmentId: String, questionId: String): ResultActionsDsl {
        return mockMvc.get("/api/v1/assessments/$assessmentId/questions/$questionId/testcases") {
            header(IdentityHeaders.PROFILE_ID, "faculty-a")
        }
    }

    private fun post(path: String, body: String): ResultActionsDsl {
        return mockMvc.post(path) {
            header(IdentityHeaders.PROFILE_ID, "faculty-a")
            contentType = MediaType.APPLICATION_JSON
            content = body
        }
    }

    private fun idOf(result: ResultActionsDsl): String {
        return result.andReturn().response.contentAsString.let { Regex("\"id\":\"([^\"]+)\"").find(it)!!.groupValues[1] }
    }
}
