package io.correcta.assessment.models

import io.correcta.assessment.entities.Assessment
import io.correcta.assessment.entities.AssessmentParameterChangeLog
import io.correcta.assessment.entities.AssessmentParameters
import io.correcta.assessment.entities.AssessmentQuestion
import io.correcta.assessment.entities.AssessmentQuestionTestcaseType
import io.correcta.assessment.entities.AssessmentResult
import io.correcta.assessment.entities.AssessmentSessionEvent
import io.correcta.assessment.entities.AssessmentSessionEventType
import io.correcta.assessment.entities.AssessmentState
import io.correcta.assessment.entities.ProgrammingLanguage
import io.correcta.assessment.models.request.AssessmentParametersRequest
import io.correcta.assessment.models.request.BulkUpdateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.request.ChangeAssessmentParametersRequest
import io.correcta.assessment.models.request.CodeDraftRequest
import io.correcta.assessment.models.request.CreateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.request.ImportAssessmentQuestionTestcasesRequest
import io.correcta.assessment.models.request.SessionTimeChangeRequest
import io.correcta.assessment.models.request.UpdateAssessmentQuestionRequest
import io.correcta.assessment.models.request.UpdateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.support.Fixtures.NOW
import io.correcta.assessment.support.Fixtures.assessment
import io.correcta.assessment.support.Fixtures.session
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/** Bean properties that the application code does not read or write yet, so only these tests cover them. */
class AccessorsTest {

  private val assessment = assessment()

  @Test
  fun `entities apply their defaults`() {
    val minimal = Assessment(ownerProfileId = "o", title = "t", slug = "s")
    assertEquals(listOf<Any?>(null, null, null, AssessmentState.DRAFT, null, null, null), listOf(minimal.id, minimal.cohortId, minimal.description, minimal.state, minimal.createdAt, minimal.updatedAt, minimal.dueDate))

    val parameters = AssessmentParameters(assessment = assessment)
    assertEquals(listOf<Any?>(false, null, true, true), listOf(parameters.canBeSubmittedAfterDueDate, parameters.maximumTimeAllowed, parameters.showCompilationErrors, parameters.canViewDefinitiveTestcaseScoringAfterSubmission))

    val question = AssessmentQuestion(assessment = assessment, position = 0, title = "t", slug = "s", statement = "s", maximumGrade = BigDecimal.ONE)
    assertTrue(question.codeDrafts.isEmpty())
    question.codeDrafts = mutableListOf()
    assertTrue(question.allowCopyPasting)
  }

  @Test
  fun `entity setters keep the value`() {
    val result = AssessmentResult(assessment = assessment, studentProfileId = "s")
    result.id = "r"
    result.assessment = assessment
    result.studentProfileId = "t"
    result.createdAt = NOW
    result.updatedAt = NOW
    assertEquals(listOf<Any?>("r", "t", NOW, NOW), listOf(result.id, result.studentProfileId, result.createdAt, result.updatedAt))
    assertSame(assessment, result.assessment)

    val log = AssessmentParameterChangeLog(assessment = assessment, changerProfileId = "f", parameterName = "p", oldValue = "o", newValue = "n")
    log.id = "l"
    log.changedAt = NOW
    assertEquals(listOf<Any?>("l", NOW), listOf(log.id, log.changedAt))

    val session = session(assessment)
    session.studentProfileId = "s"
    session.createdAt = NOW
    session.startedAt = NOW
    assertEquals(listOf<Any?>("s", NOW, NOW), listOf(session.studentProfileId, session.createdAt, session.startedAt))

    val event = AssessmentSessionEvent(session = session, type = AssessmentSessionEventType.STARTED, occurredAt = NOW, receivedAt = NOW)
    assertNull(event.id)
    event.id = "e"
    event.session = session
    event.type = AssessmentSessionEventType.RESUMED
    event.occurredAt = NOW
    event.receivedAt = NOW
    event.actorProfileId = "a"
    event.detail = "d"
    assertSame(session, event.session)
    assertEquals(listOf<Any?>("e", AssessmentSessionEventType.RESUMED, NOW, NOW, "a", "d"), listOf(event.id, event.type, event.occurredAt, event.receivedAt, event.actorProfileId, event.detail))
  }

  @Test
  fun `request accessors keep the value`() {
    val parameters = AssessmentParametersRequest(true, 10, false, false)
    assertEquals(listOf<Any?>(true, 10, false, false), listOf(parameters.canBeSubmittedAfterDueDate, parameters.maximumTimeAllowed, parameters.showCompilationErrors, parameters.canViewDefinitiveTestcaseScoringAfterSubmission))

    val change = ChangeAssessmentParametersRequest(true, 10, false, false)
    assertEquals(listOf<Any?>(true, 10, false, false), listOf(change.canBeSubmittedAfterDueDate, change.maximumTimeAllowed, change.showCompilationErrors, change.canViewDefinitiveTestcaseScoringAfterSubmission))

    val draft = CodeDraftRequest(null, "c", "h")
    draft.language = ProgrammingLanguage.C
    assertEquals(listOf<Any?>(ProgrammingLanguage.C, "c", "h"), listOf(draft.language, draft.code, draft.harness))

    val bulk = BulkUpdateAssessmentQuestionTestcaseRequest(listOf("t"), null, null, null)
    bulk.type = AssessmentQuestionTestcaseType.PUBLIC
    assertEquals(AssessmentQuestionTestcaseType.PUBLIC, bulk.type)

    val import = ImportAssessmentQuestionTestcasesRequest(type = null)
    import.type = AssessmentQuestionTestcaseType.PRIVATE
    assertEquals(AssessmentQuestionTestcaseType.PRIVATE, import.type)

    val create = CreateAssessmentQuestionTestcaseRequest(expectedOutput = "1", type = null)
    create.expectedOutput = "2"
    create.type = AssessmentQuestionTestcaseType.EVALUATION
    assertEquals(listOf<Any?>("2", AssessmentQuestionTestcaseType.EVALUATION), listOf(create.expectedOutput, create.type))

    val update = UpdateAssessmentQuestionTestcaseRequest(position = null, expectedOutput = null, type = null)
    update.position = 1
    update.expectedOutput = "o"
    update.type = AssessmentQuestionTestcaseType.PUBLIC
    assertEquals(listOf<Any?>(1, "o", AssessmentQuestionTestcaseType.PUBLIC), listOf(update.position, update.expectedOutput, update.type))

    val question = UpdateAssessmentQuestionRequest(position = null, title = "t", statement = "s", maximumGrade = null)
    question.position = 2
    question.maximumGrade = BigDecimal.TEN
    assertEquals(listOf<Any?>(2, BigDecimal.TEN), listOf(question.position, question.maximumGrade))

    val time = SessionTimeChangeRequest()
    time.extraMinutes = 5
    assertEquals(5, time.extraMinutes)
  }
}