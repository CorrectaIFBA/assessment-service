package io.correcta.assessment.models

import io.correcta.assessment.entities.AssessmentQuestionTestcaseType
import io.correcta.assessment.entities.AssessmentResult
import io.correcta.assessment.entities.AssessmentSessionEvent
import io.correcta.assessment.entities.AssessmentSessionEventType
import io.correcta.assessment.entities.AssessmentSessionState
import io.correcta.assessment.entities.AssessmentState
import io.correcta.assessment.entities.ProgrammingLanguage
import io.correcta.assessment.models.request.AssessmentFilterRequest
import io.correcta.assessment.models.request.AssessmentParametersRequest
import io.correcta.assessment.models.request.BulkUpdateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.request.ChangeAssessmentParametersRequest
import io.correcta.assessment.models.request.CodeDraftRequest
import io.correcta.assessment.models.request.CreateAssessmentQuestionRequest
import io.correcta.assessment.models.request.CreateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.request.CreateAssessmentRequest
import io.correcta.assessment.models.request.ImportAssessmentQuestionTestcasesRequest
import io.correcta.assessment.models.request.PaginationRequest
import io.correcta.assessment.models.request.SessionFilterRequest
import io.correcta.assessment.models.request.SessionTimeChangeRequest
import io.correcta.assessment.models.request.UpdateAssessmentQuestionRequest
import io.correcta.assessment.models.request.UpdateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.request.UpdateAssessmentRequest
import io.correcta.assessment.models.response.GetAssessmentQuestionTestcaseResponse
import io.correcta.assessment.models.response.GetAssessmentResponse
import io.correcta.assessment.models.response.GetAssessmentSessionEventResponse
import io.correcta.assessment.models.response.GetAssessmentSessionResponse
import io.correcta.assessment.models.response.GetAssessmentSessionSummaryResponse
import io.correcta.assessment.models.response.GetInternalAssessmentQuestionResponse
import io.correcta.assessment.models.response.GetSessionSubmissionStatusResponse
import io.correcta.assessment.models.response.PagedResponse
import io.correcta.assessment.models.response.error.ErrorResponse
import io.correcta.assessment.models.response.error.ValidationErrorResponse
import io.correcta.assessment.models.response.isLate
import io.correcta.assessment.repositories.AssessmentQuestionRepository
import io.correcta.assessment.support.Fixtures.NOW
import io.correcta.assessment.support.Fixtures.assessment
import io.correcta.assessment.support.Fixtures.parameters
import io.correcta.assessment.support.Fixtures.question
import io.correcta.assessment.support.Fixtures.session
import io.correcta.assessment.support.Fixtures.testcase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal

class ModelsTest {

  private val assessment = assessment()

  @Test
  fun `request defaults`() {
    assertEquals(listOf(0, 20, "createdAt", "desc"), PaginationRequest().let { listOf(it.page, it.size, it.sortBy, it.sortDirection) })
    assertEquals(listOf(null, null, null), AssessmentFilterRequest().let { listOf(it.cohortId, it.title, it.state) })
    assertNull(SessionFilterRequest().state)
    assertEquals(listOf(5, 256), ImportAssessmentQuestionTestcasesRequest(type = AssessmentQuestionTestcaseType.PUBLIC).let { listOf(it.timeLimitSeconds, it.memoryLimitMB) })
    assertEquals(listOf<Any?>(null, "", 5, 256), CreateAssessmentQuestionTestcaseRequest(expectedOutput = "1", type = null).let { listOf(it.position, it.input, it.timeLimitSeconds, it.memoryLimitMB) })
    assertEquals(listOf<Any?>("", 5, 256), UpdateAssessmentQuestionTestcaseRequest(position = 0, expectedOutput = null, type = null).let { listOf(it.input, it.timeLimitSeconds, it.memoryLimitMB) })
    assertEquals(listOf<Any?>(null, null), SessionTimeChangeRequest().let { listOf(it.extraMinutes, it.reason) })
    assertEquals(listOf<Any?>(null, null), UpdateAssessmentRequest(title = "T").let { listOf(it.description, it.dueDate) })
  }

  @Test
  fun `question requests default to no position and slug and allow copy pasting`() {
    val create = CreateAssessmentQuestionRequest(title = "T", statement = "S", maximumGrade = BigDecimal.ONE)
    assertEquals(listOf<Any?>(null, null, emptyList<CodeDraftRequest>(), true), listOf(create.position, create.slug, create.codeDrafts, create.allowCopyPasting))

    val update = UpdateAssessmentQuestionRequest(position = 0, title = "T", statement = "S", maximumGrade = BigDecimal.ONE)
    assertEquals(listOf<Any?>(null, emptyList<CodeDraftRequest>(), true), listOf(update.slug, update.codeDrafts, update.allowCopyPasting))

    val draft = CodeDraftRequest(ProgrammingLanguage.C).toCodeDraft()
    assertEquals(listOf<Any>(ProgrammingLanguage.C, "", ""), listOf(draft.language, draft.code, draft.harness))
  }

  @Test
  fun `bulk test case requests keep their fields`() {
    val request = BulkUpdateAssessmentQuestionTestcaseRequest(listOf("t"), 1, 16, AssessmentQuestionTestcaseType.PRIVATE)

    assertEquals(listOf<Any?>(listOf("t"), 1, 16, AssessmentQuestionTestcaseType.PRIVATE), listOf(request.testCaseIds, request.timeLimitSeconds, request.memoryLimitMB, request.type))
  }

  @Test
  fun `assessment requests build their parameters`() {
    val defaults = AssessmentParametersRequest().toAssessmentParameters(assessment)
    assertEquals(listOf<Any?>(false, null, true, true), listOf(defaults.canBeSubmittedAfterDueDate, defaults.maximumTimeAllowed, defaults.showCompilationErrors, defaults.canViewDefinitiveTestcaseScoringAfterSubmission))

    val create = CreateAssessmentRequest(title = "T", cohortId = "c")
    assertEquals(listOf<Any?>(null, null, null, null), listOf(create.slug, create.description, create.dueDate, create.parameters))
  }

  @Test
  fun `parameter changes apply only the given values`() {
    val parameters = parameters(assessment, maximumTimeAllowed = 30)

    ChangeAssessmentParametersRequest().applyChangesTo(parameters)
    assertEquals(30, parameters.maximumTimeAllowed)

    ChangeAssessmentParametersRequest(true, 45, false, false).applyChangesTo(parameters)
    assertEquals(listOf<Any?>(true, 45, false, false), listOf(parameters.canBeSubmittedAfterDueDate, parameters.maximumTimeAllowed, parameters.showCompilationErrors, parameters.canViewDefinitiveTestcaseScoringAfterSubmission))

    ChangeAssessmentParametersRequest(maximumTimeAllowed = 0).applyChangesTo(parameters)
    assertNull(parameters.maximumTimeAllowed)
  }

  @Test
  fun `results keep the grade`() {
    val result = AssessmentResult(id = "r", assessment = assessment, studentProfileId = "s", createdAt = NOW, updatedAt = NOW)
    assertEquals(listOf<Any?>(null, null, null, null), listOf(result.finalScore, result.generalComment, result.gradedByProfileId, result.gradedAt))

    result.finalScore = BigDecimal.TEN
    result.generalComment = "Bom"
    result.gradedByProfileId = "faculty-a"
    result.gradedAt = NOW
    assertEquals(listOf<Any?>(BigDecimal.TEN, "Bom", "faculty-a", NOW), listOf(result.finalScore, result.generalComment, result.gradedByProfileId, result.gradedAt))
  }

  @Test
  fun `event responses copy every field`() {
    val event = AssessmentSessionEvent(id = "e", session = session(assessment), type = AssessmentSessionEventType.EXTENDED, occurredAt = NOW, receivedAt = NOW, actorProfileId = "f", detail = "d")
    val eventResponse = GetAssessmentSessionEventResponse.fromEvent(event)
    assertEquals(
      listOf<Any?>("e", AssessmentSessionEventType.EXTENDED, NOW.toString(), NOW.toString(), "f", "d"),
      listOf(eventResponse.id, eventResponse.type, eventResponse.occurredAt, eventResponse.receivedAt, eventResponse.actorProfileId, eventResponse.detail),
    )
  }

  @Test
  fun `assessment responses default the question totals`() {
    val response = GetAssessmentResponse.fromAssessment(assessment)

    assertEquals(listOf<Any?>(0L, BigDecimal.ZERO, null, null, null), listOf(response.questionCount, response.totalGrade, response.parameters, response.score, response.mySession))
    assertEquals(listOf<Any?>("faculty-a", "cohort-1", null, NOW.toString()), listOf(response.ownerProfileId, response.cohortId, response.description, response.updatedAt))
    assertEquals(NOW.toString(), GetAssessmentResponse.fromAssessment(assessment(dueDate = NOW)).dueDate)

    val emptySummary = Mockito.mock(AssessmentQuestionRepository.QuestionSummary::class.java)
    assertEquals(BigDecimal.ZERO, GetAssessmentResponse.fromAssessment(assessment, questions = emptySummary).totalGrade)

    val parameters = GetAssessmentResponse.fromAssessment(assessment, parameters(assessment)).parameters!!
    assertEquals(listOf<Any?>("assessment-1", true, NOW.toString(), NOW.toString()), listOf(parameters.assessmentId, parameters.showCompilationErrors, parameters.createdAt, parameters.updatedAt))
  }

  @Test
  fun `test case responses hide what the type hides from students`() {
    val question = question(assessment)
    val private = testcase(question, type = AssessmentQuestionTestcaseType.PRIVATE)

    val student = GetAssessmentQuestionTestcaseResponse.fromTestcase(private, isFaculty = false)
    assertEquals(listOf<Any?>(null, null), listOf(student.input, student.expectedOutput))

    val faculty = GetAssessmentQuestionTestcaseResponse.fromTestcase(private, isFaculty = true)
    assertEquals(listOf<Any?>("1 2", "3", "question-1", NOW.toString(), NOW.toString()), listOf(faculty.input, faculty.expectedOutput, faculty.questionId, faculty.createdAt, faculty.updatedAt))

    val internal = GetInternalAssessmentQuestionResponse("q", "a", ProgrammingLanguage.C, "h", listOf(faculty))
    assertEquals(listOf<Any?>("q", "a", ProgrammingLanguage.C, "h", 1), listOf(internal.id, internal.assessmentId, internal.language, internal.harness, internal.testcases.size))
  }

  @Test
  fun `session responses tell whether the session is late`() {
    val onTime = session(assessment, expiresAt = NOW, lastHeartbeatAt = null, finishedAt = NOW)
    assertFalse(onTime.isLate(NOW))

    assessment.dueDate = NOW.minusSeconds(1)
    assertTrue(onTime.isLate(NOW))
    assertFalse(session(assessment, finishedAt = NOW.minusSeconds(2)).isLate(NOW))
    assertTrue(session(assessment).isLate(NOW))

    val response = GetAssessmentSessionResponse.fromSession(onTime, NOW, canViewDefinitiveTestcaseScoringAfterSubmission = true)
    assertEquals(
      listOf<Any?>("Lista 1", "lista-1", "cohort-1", "student-a", NOW.toString(), NOW.toString(), NOW.toString(), null, 0, NOW.toString()),
      listOf(response.assessmentTitle, response.assessmentSlug, response.cohortId, response.studentProfileId, response.startedAt, response.expiresAt, response.finishedAt, response.lastHeartbeatAt, response.openSeconds, response.serverNow),
    )

    val summary = GetAssessmentSessionSummaryResponse.fromSession(onTime, NOW)
    assertEquals(listOf<Any?>(AssessmentSessionState.IN_PROGRESS, NOW.toString(), NOW.toString(), NOW.toString(), true, NOW.toString()), listOf(summary.state, summary.startedAt, summary.expiresAt, summary.finishedAt, summary.late, summary.serverNow))

    val running = GetAssessmentSessionSummaryResponse.fromSession(session(assessment), NOW)
    assertEquals(listOf<Any?>(null, null), listOf(running.expiresAt, running.finishedAt))
  }

  @Test
  fun `small responses keep their fields`() {
    val status = GetSessionSubmissionStatusResponse("s", AssessmentSessionState.EXPIRED, "e", "f", false, true)
    assertEquals(listOf<Any?>("s", AssessmentSessionState.EXPIRED, "e", "f", false, true), listOf(status.sessionId, status.state, status.expiresAt, status.finishedAt, status.acceptsSubmissions, status.late))

    val page = PagedResponse(listOf(1), 0, 20, 1, 1)
    assertEquals(listOf<Any?>(listOf(1), 0, 20, 1L, 1, null, null), listOf(page.content, page.page, page.size, page.totalElements, page.totalPages, page.sortBy, page.sortDirection))

    assertEquals("m", ErrorResponse("m").message)
    assertEquals(mapOf("f" to "e"), ValidationErrorResponse("m", mapOf("f" to "e")).errors)
  }
}