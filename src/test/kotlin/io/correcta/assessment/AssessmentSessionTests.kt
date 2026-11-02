package io.correcta.assessment

import io.correcta.assessment.clients.CohortClient
import io.correcta.assessment.models.response.clients.CohortMembershipResponse
import io.correcta.assessment.security.models.IdentityHeaders
import java.time.Clock
import java.time.Duration
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
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AssessmentSessionTests(
    @Autowired val mockMvc: MockMvc,
) {

    @MockitoBean
    lateinit var cohortClient: CohortClient

    @MockitoBean
    lateinit var clock: Clock

    private var now: Instant = Instant.parse("2030-03-10T13:00:00Z")

    @BeforeEach
    fun mockMembershipsAndClock() {
        `when`(cohortClient.getCurrentMembership("cohort-1", "faculty-a"))
            .thenReturn(CohortMembershipResponse("cohort-1", "ACTIVE", "faculty-a", "FACULTY"))
        listOf("student-a", "student-b").forEach {
            `when`(cohortClient.getCurrentMembership("cohort-1", it))
                .thenReturn(CohortMembershipResponse("cohort-1", "ACTIVE", it, "STUDENT"))
        }
        `when`(clock.instant()).thenAnswer { now }
    }

    @Test
    fun `starting opens the questions, resuming answers the same session and faculty never start`() {
        val assessmentId = publishedAssessment()

        getQuestions(assessmentId).andExpect { status { isForbidden() } }

        val sessionId = idOf(start(assessmentId).andExpect {
            status { isCreated() }
            jsonPath("$.state") { value("IN_PROGRESS") }
            jsonPath("$.expiresAt") { doesNotExist() }
        })

        getQuestions(assessmentId).andExpect { status { isOk() } }

        start(assessmentId).andExpect {
            status { isOk() }
            jsonPath("$.id") { value(sessionId) }
        }

        start(assessmentId, profileId = "faculty-a").andExpect { status { isForbidden() } }

        getAssessment(assessmentId).andExpect {
            jsonPath("$.mySession.id") { value(sessionId) }
            jsonPath("$.mySession.state") { value("IN_PROGRESS") }
        }
        getAssessment(assessmentId, profileId = "faculty-a").andExpect { jsonPath("$.mySession") { doesNotExist() } }
    }

    @Test
    fun `the time limit runs from the start and ends the session`() {
        val assessmentId = publishedAssessment(parameters = """{"maximumTimeAllowed": 30}""")

        start(assessmentId).andExpect { jsonPath("$.expiresAt") { value("2030-03-10T13:30:00Z") } }

        now = now.plus(Duration.ofMinutes(31))

        heartbeat(assessmentId).andExpect {
            status { isOk() }
            jsonPath("$.state") { value("EXPIRED") }
            jsonPath("$.finishedAt") { value("2030-03-10T13:30:00Z") }
        }
        submit(assessmentId).andExpect { status { isConflict() } }
        start(assessmentId).andExpect { status { isConflict() } }
    }

    @Test
    fun `without late submissions the due date closes the assessment`() {
        val assessmentId = publishedAssessment(
            dueDate = "2030-03-10T14:00:00Z",
            parameters = """{"maximumTimeAllowed": 120}"""
        )

        start(assessmentId).andExpect { jsonPath("$.expiresAt") { value("2030-03-10T14:00:00Z") } }

        now = Instant.parse("2030-03-10T14:00:01Z")
        start(assessmentId, profileId = "student-b").andExpect { status { isConflict() } }

        val lateAssessmentId = publishedAssessment(
            dueDate = "2030-03-10T14:00:00Z",
            parameters = """{"canBeSubmittedAfterDueDate": true}"""
        )

        start(lateAssessmentId, profileId = "student-b").andExpect {
            status { isCreated() }
            jsonPath("$.late") { value(true) }
        }
    }

    @Test
    fun `compilation errors while coding are a parameter, on unless turned off`() {
        createAssessment().andExpect { jsonPath("$.parameters.showCompilationErrors") { value(true) } }

        val assessmentId = idOf(createAssessment(parameters = """{"showCompilationErrors": false}"""))
        getAssessment(assessmentId, profileId = "faculty-a").andExpect { jsonPath("$.parameters.showCompilationErrors") { value(false) } }

        changeParameters(assessmentId, """{"showCompilationErrors": true}""").andExpect {
            jsonPath("$.parameters.showCompilationErrors") { value(true) }
        }
    }

    @Test
    fun `heartbeats sum the time the page was open`() {
        val assessmentId = publishedAssessment()

        start(assessmentId)
        now = now.plusSeconds(30)
        heartbeat(assessmentId)
        now = now.plusSeconds(30)
        heartbeat(assessmentId)
        now = now.plusSeconds(300)
        heartbeat(assessmentId).andExpect {
            jsonPath("$.state") { value("IN_PROGRESS") }
            jsonPath("$.openSeconds") { value(60) }
        }
    }

    @Test
    fun `archiving finishes running sessions`() {
        val assessmentId = publishedAssessment()
        start(assessmentId)
        start(assessmentId, profileId = "student-b")
        submit(assessmentId, profileId = "student-b")

        mockMvc.post("/api/v1/assessments/$assessmentId/archive") { header(IdentityHeaders.PROFILE_ID, "faculty-a") }
            .andExpect { status { isOk() } }

        mockMvc.get("/api/v1/assessments/$assessmentId/sessions/summary") { header(IdentityHeaders.PROFILE_ID, "faculty-a") }
            .andExpect {
                jsonPath("$.EXPIRED") { value(1) }
                jsonPath("$.SUBMITTED") { value(1) }
                jsonPath("$.IN_PROGRESS") { value(0) }
            }
    }

    @Test
    fun `faculty extend, close and reopen sessions`() {
        val assessmentId = publishedAssessment(parameters = """{"maximumTimeAllowed": 30}""")
        val sessionId = idOf(start(assessmentId))

        facultyAction(assessmentId, sessionId, "extend", """{"extraMinutes": 10, "reason": "Queda de energia"}""").andExpect {
            jsonPath("$.expiresAt") { value("2030-03-10T13:40:00Z") }
        }
        facultyAction(assessmentId, sessionId, "close").andExpect { jsonPath("$.state") { value("SUBMITTED") } }
        facultyAction(assessmentId, sessionId, "extend", """{"extraMinutes": 10}""").andExpect { status { isConflict() } }

        now = now.plus(Duration.ofHours(1))
        facultyAction(assessmentId, sessionId, "reopen", """{"extraMinutes": 15}""").andExpect {
            jsonPath("$.state") { value("IN_PROGRESS") }
            jsonPath("$.expiresAt") { value("2030-03-10T14:15:00Z") }
        }

        facultyAction(assessmentId, sessionId, "close", profileId = "student-a").andExpect { status { isForbidden() } }
        mockMvc.get("/api/v1/assessments/$assessmentId/sessions") { header(IdentityHeaders.PROFILE_ID, "student-a") }
            .andExpect { status { isForbidden() } }
    }

    @Test
    fun `started sessions keep the grade from changing`() {
        val assessmentId = idOf(createAssessment())
        val questionId = idOf(createQuestion(assessmentId))
        createQuestion(assessmentId, title = "Produto")
        publish(assessmentId)

        start(assessmentId)

        mockMvc.delete("/api/v1/assessments/$assessmentId/questions/$questionId") { header(IdentityHeaders.PROFILE_ID, "faculty-a") }
            .andExpect { status { isConflict() } }

        editQuestion(assessmentId, questionId, maximumGrade = "5").andExpect { status { isConflict() } }
        editQuestion(assessmentId, questionId, maximumGrade = "2.5").andExpect { status { isOk() } }
    }

    @Test
    fun `submission-service learns whether the student may submit`() {
        val assessmentId = publishedAssessment(parameters = """{"maximumTimeAllowed": 30}""")
        start(assessmentId)

        submissionStatus(assessmentId).andExpect { jsonPath("$.acceptsSubmissions") { value(true) } }

        now = Instant.parse("2030-03-10T13:30:03Z")
        submissionStatus(assessmentId).andExpect {
            jsonPath("$.state") { value("EXPIRED") }
            jsonPath("$.acceptsSubmissions") { value(true) }
        }

        now = Instant.parse("2030-03-10T13:30:10Z")
        submissionStatus(assessmentId).andExpect { jsonPath("$.acceptsSubmissions") { value(false) } }

        mockMvc.get("/api/v1/internal/assessments/$assessmentId/sessions/student-b").andExpect { status { isNotFound() } }
    }

    private fun publishedAssessment(dueDate: String? = null, parameters: String? = null): String {
        val assessmentId = idOf(createAssessment(dueDate, parameters))
        createQuestion(assessmentId)
        publish(assessmentId).andExpect { status { isOk() } }

        return assessmentId
    }

    private fun createAssessment(dueDate: String? = null, parameters: String? = null): ResultActionsDsl {
        val dueDateField = dueDate?.let { """"dueDate": "$it",""" } ?: ""
        val parametersField = parameters?.let { """"parameters": $it,""" } ?: ""

        return mockMvc.post("/api/v1/assessments") {
            header(IdentityHeaders.PROFILE_ID, "faculty-a")
            contentType = MediaType.APPLICATION_JSON
            content = """{$dueDateField $parametersField "title": "Prova ${System.nanoTime()}", "cohortId": "cohort-1"}"""
        }
    }

    private fun createQuestion(assessmentId: String, title: String = "Soma"): ResultActionsDsl {
        return mockMvc.post("/api/v1/assessments/$assessmentId/questions") {
            header(IdentityHeaders.PROFILE_ID, "faculty-a")
            contentType = MediaType.APPLICATION_JSON
            content = """{"title": "$title", "statement": "Some dois números.", "codeDrafts": [{"language": "C", "code": ""}], "maximumGrade": 2.5}"""
        }
    }

    private fun editQuestion(assessmentId: String, questionId: String, maximumGrade: String): ResultActionsDsl {
        return mockMvc.post("/api/v1/assessments/$assessmentId/questions/$questionId/edit") {
            header(IdentityHeaders.PROFILE_ID, "faculty-a")
            contentType = MediaType.APPLICATION_JSON
            content = """{"position": 0, "title": "Soma", "statement": "Some três números.", "codeDrafts": [{"language": "C", "code": ""}], "maximumGrade": $maximumGrade}"""
        }
    }

    private fun publish(assessmentId: String): ResultActionsDsl {
        return mockMvc.post("/api/v1/assessments/$assessmentId/publish") { header(IdentityHeaders.PROFILE_ID, "faculty-a") }
    }

    private fun changeParameters(assessmentId: String, body: String): ResultActionsDsl {
        return mockMvc.patch("/api/v1/assessments/$assessmentId/parameters") {
            header(IdentityHeaders.PROFILE_ID, "faculty-a")
            contentType = MediaType.APPLICATION_JSON
            content = body
        }
    }

    private fun getAssessment(assessmentId: String, profileId: String = "student-a"): ResultActionsDsl {
        return mockMvc.get("/api/v1/assessments/$assessmentId") { header(IdentityHeaders.PROFILE_ID, profileId) }
    }

    private fun getQuestions(assessmentId: String): ResultActionsDsl {
        return mockMvc.get("/api/v1/assessments/$assessmentId/questions") { header(IdentityHeaders.PROFILE_ID, "student-a") }
    }

    private fun start(assessmentId: String, profileId: String = "student-a"): ResultActionsDsl {
        return mockMvc.post("/api/v1/assessments/$assessmentId/sessions/me/start") { header(IdentityHeaders.PROFILE_ID, profileId) }
    }

    private fun heartbeat(assessmentId: String): ResultActionsDsl {
        return mockMvc.post("/api/v1/assessments/$assessmentId/sessions/me/heartbeat") { header(IdentityHeaders.PROFILE_ID, "student-a") }
    }

    private fun submit(assessmentId: String, profileId: String = "student-a"): ResultActionsDsl {
        return mockMvc.post("/api/v1/assessments/$assessmentId/sessions/me/submit") { header(IdentityHeaders.PROFILE_ID, profileId) }
    }

    private fun facultyAction(
        assessmentId: String,
        sessionId: String,
        action: String,
        body: String? = null,
        profileId: String = "faculty-a",
    ): ResultActionsDsl {
        return mockMvc.post("/api/v1/assessments/$assessmentId/sessions/$sessionId/$action") {
            header(IdentityHeaders.PROFILE_ID, profileId)
            body?.let {
                contentType = MediaType.APPLICATION_JSON
                content = it
            }
        }
    }

    private fun submissionStatus(assessmentId: String): ResultActionsDsl {
        return mockMvc.get("/api/v1/internal/assessments/$assessmentId/sessions/student-a")
    }

    private fun idOf(result: ResultActionsDsl): String {
        return result.andReturn().response.contentAsString.let { Regex("\"id\":\"([^\"]+)\"").find(it)!!.groupValues[1] }
    }
}
