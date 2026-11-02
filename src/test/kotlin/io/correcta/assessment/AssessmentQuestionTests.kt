package io.correcta.assessment

import io.correcta.assessment.clients.CohortClient
import io.correcta.assessment.entities.AssessmentSession
import io.correcta.assessment.models.response.clients.CohortMembershipResponse
import io.correcta.assessment.repositories.AssessmentRepository
import io.correcta.assessment.repositories.AssessmentSessionRepository
import io.correcta.assessment.security.models.IdentityHeaders
import java.time.Instant
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AssessmentQuestionTests(
    @Autowired val mockMvc: MockMvc,
    @Autowired val assessmentRepository: AssessmentRepository,
    @Autowired val assessmentSessionRepository: AssessmentSessionRepository,
) {

    @MockitoBean
    lateinit var cohortClient: CohortClient

    @BeforeEach
    fun mockMemberships() {
        `when`(cohortClient.getCurrentMembership("cohort-1", "faculty-a"))
            .thenReturn(CohortMembershipResponse("cohort-1", "ACTIVE", "faculty-a", "FACULTY"))
        `when`(cohortClient.getCurrentMembership("cohort-1", "student-a"))
            .thenReturn(CohortMembershipResponse("cohort-1", "ACTIVE", "student-a", "STUDENT"))
    }

    @Test
    fun `assessment cannot be published without a question`() {
        val assessmentId = createAssessment()

        publish(assessmentId).andExpect { status { isConflict() } }

        createQuestion(assessmentId, "C").andExpect { status { isCreated() } }

        publish(assessmentId).andExpect {
            status { isOk() }
            jsonPath("$.state") { value("PUBLISHED") }
        }
    }

    @Test
    fun `allowed languages are limited to the supported ones`() {
        val assessmentId = createAssessment()

        createQuestion(assessmentId, "C").andExpect {
            status { isCreated() }
            jsonPath("$.codeDrafts[0].language") { value("C") }
        }

        listOf("PYTHON", "JAVA", "RUST").forEach { language ->
            createQuestion(assessmentId, language).andExpect { status { isBadRequest() } }
        }
    }

    @Test
    fun `a question allows one or more languages, each once`() {
        val assessmentId = createAssessment()

        createQuestion(assessmentId, "C").andExpect {
            status { isCreated() }
            jsonPath("$.codeDrafts.length()") { value(1) }
        }

        createQuestion(assessmentId).andExpect { status { isBadRequest() } }
        createQuestion(assessmentId, "C", "C").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `editing a question replaces its allowed languages`() {
        val assessmentId = createAssessment()
        val questionId = idOf(createQuestion(assessmentId, "C"))

        mockMvc.post("/api/v1/assessments/$assessmentId/questions/$questionId/edit") {
            header(IdentityHeaders.PROFILE_ID, "faculty-a")
            contentType = MediaType.APPLICATION_JSON
            content = """{"position": 0, "title": "Soma de três", "statement": "Some três números.", "codeDrafts": [{"language": "C", "code": "int main() {}"}], "maximumGrade": 3}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.codeDrafts.length()") { value(1) }
            jsonPath("$.codeDrafts[0].code") { value("int main() {}") }
        }
    }

    @Test
    fun `the assessment reports how many questions it has and what they are worth`() {
        val assessmentId = createAssessment()

        createQuestion(assessmentId, "C")
        createQuestion(assessmentId, "C")

        mockMvc.get("/api/v1/assessments/$assessmentId") {
            header(IdentityHeaders.PROFILE_ID, "faculty-a")
        }.andExpect {
            status { isOk() }
            jsonPath("$.questionCount") { value(2) }
            jsonPath("$.totalGrade") { value(5.0) }
            jsonPath("$.score") { doesNotExist() }
        }
    }

    @Test
    fun `questions are appended in order and positions cannot collide`() {
        val assessmentId = createAssessment()

        createQuestion(assessmentId, "C").andExpect { jsonPath("$.position") { value(0) } }
        createQuestion(assessmentId, "C").andExpect { jsonPath("$.position") { value(1) } }
        createQuestion(assessmentId, "C", position = 1).andExpect { status { isConflict() } }
    }

    @Test
    fun `students only see questions of published assessments and cannot manage them`() {
        val assessmentId = createAssessment()
        val questionId = idOf(createQuestion(assessmentId, "C"))

        getQuestions(assessmentId, "student-a").andExpect { status { isNotFound() } }
        createQuestion(assessmentId, "C", profileId = "student-a").andExpect { status { isForbidden() } }

        publish(assessmentId)

        getQuestions(assessmentId, "student-a").andExpect { status { isForbidden() } }

        assessmentSessionRepository.save(
            AssessmentSession(
                assessment = assessmentRepository.findById(assessmentId).get(),
                studentProfileId = "student-a",
                startedAt = Instant.now(),
            )
        )

        getQuestions(assessmentId, "student-a").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
        }

        deleteQuestion(assessmentId, questionId).andExpect { status { isConflict() } }
    }

    @Test
    fun `assessments are addressed by a slug unique inside their cohort`() {
        val assessmentId = idOf(
            postAssessment("""{"title": "Árvores Binárias", "cohortId": "cohort-1"}""").andExpect {
                status { isCreated() }
                jsonPath("$.slug") { value("arvores-binarias") }
            }
        )

        postAssessment("""{"title": "Árvores Binárias", "cohortId": "cohort-1"}""").andExpect {
            jsonPath("$.slug") { value("arvores-binarias-2") }
        }
        postAssessment("""{"title": "Outra", "slug": "arvores-binarias", "cohortId": "cohort-1"}""").andExpect {
            status { isConflict() }
        }
        postAssessment("""{"title": "Outra", "slug": "Com Espaço", "cohortId": "cohort-1"}""").andExpect {
            status { isBadRequest() }
        }

        mockMvc.post("/api/v1/assessments/$assessmentId/edit") {
            header(IdentityHeaders.PROFILE_ID, "faculty-a")
            contentType = MediaType.APPLICATION_JSON
            content = """{"title": "Árvores", "slug": "arvores"}"""
        }.andExpect { jsonPath("$.slug") { value("arvores") } }

        mockMvc.get("/api/v1/assessments/by-slug/cohort-1/arvores") {
            header(IdentityHeaders.PROFILE_ID, "faculty-a")
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(assessmentId) }
        }

        mockMvc.get("/api/v1/assessments/by-slug/cohort-1/arvores-binarias-3") {
            header(IdentityHeaders.PROFILE_ID, "faculty-a")
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `questions get a slug from their title, unique inside the assessment`() {
        val assessmentId = createAssessment()

        createQuestion(assessmentId, "C", title = "Soma de Vetores").andExpect {
            jsonPath("$.title") { value("Soma de Vetores") }
            jsonPath("$.slug") { value("soma-de-vetores") }
        }
        createQuestion(assessmentId, "C", title = "Soma de Vetores").andExpect { jsonPath("$.slug") { value("soma-de-vetores-2") } }
        createQuestion(assessmentId, "C", slug = "soma-de-vetores").andExpect { status { isConflict() } }

        createQuestion(assessmentId, "C", slug = "new").andExpect { status { isBadRequest() } }
        createQuestion(assessmentId, "C", title = "New").andExpect { jsonPath("$.slug") { value("new-2") } }
    }

    private fun createAssessment(): String {
        return idOf(postAssessment("""{"title": "Lista 1", "cohortId": "cohort-1"}""").andExpect { status { isCreated() } })
    }

    private fun postAssessment(body: String): ResultActionsDsl {
        return mockMvc.post("/api/v1/assessments") {
            header(IdentityHeaders.PROFILE_ID, "faculty-a")
            contentType = MediaType.APPLICATION_JSON
            content = body
        }
    }

    private fun createQuestion(
        assessmentId: String,
        vararg languages: String,
        position: Int? = null,
        profileId: String = "faculty-a",
        title: String = "Soma",
        slug: String? = null,
    ): ResultActionsDsl {
        val positionField = position?.let { """"position": $it,""" } ?: ""
        val slugField = slug?.let { """"slug": "$it",""" } ?: ""

        return mockMvc.post("/api/v1/assessments/$assessmentId/questions") {
            header(IdentityHeaders.PROFILE_ID, profileId)
            contentType = MediaType.APPLICATION_JSON
            content = """{$positionField $slugField "title": "$title", "statement": "Some dois números.", ${codeDraftsJson(*languages)}, "maximumGrade": 2.5}"""
        }
    }

    private fun codeDraftsJson(vararg languages: String): String {
        return languages.joinToString(prefix = """"codeDrafts": [""", postfix = "]") { """{"language": "$it", "code": ""}""" }
    }

    private fun publish(assessmentId: String): ResultActionsDsl {
        return mockMvc.post("/api/v1/assessments/$assessmentId/publish") {
            header(IdentityHeaders.PROFILE_ID, "faculty-a")
        }
    }

    private fun getQuestions(assessmentId: String, profileId: String): ResultActionsDsl {
        return mockMvc.get("/api/v1/assessments/$assessmentId/questions") {
            header(IdentityHeaders.PROFILE_ID, profileId)
        }
    }

    private fun deleteQuestion(assessmentId: String, questionId: String): ResultActionsDsl {
        return mockMvc.delete("/api/v1/assessments/$assessmentId/questions/$questionId") {
            header(IdentityHeaders.PROFILE_ID, "faculty-a")
        }
    }

    private fun idOf(result: ResultActionsDsl): String {
        return result.andReturn().response.contentAsString.let { Regex("\"id\":\"([^\"]+)\"").find(it)!!.groupValues[1] }
    }
}
