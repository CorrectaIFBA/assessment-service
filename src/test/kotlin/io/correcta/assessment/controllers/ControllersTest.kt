package io.correcta.assessment.controllers

import io.correcta.assessment.controllers.impl.AssessmentQuestionTestcasesControllerImpl
import io.correcta.assessment.controllers.impl.AssessmentQuestionsControllerImpl
import io.correcta.assessment.controllers.impl.AssessmentSessionsControllerImpl
import io.correcta.assessment.controllers.impl.AssessmentsControllerImpl
import io.correcta.assessment.controllers.impl.InternalAssessmentQuestionsControllerImpl
import io.correcta.assessment.controllers.impl.InternalAssessmentSessionsControllerImpl
import io.correcta.assessment.controllers.impl.MyAssessmentSessionsControllerImpl
import io.correcta.assessment.entities.AssessmentQuestionTestcaseType
import io.correcta.assessment.entities.ProgrammingLanguage
import io.correcta.assessment.models.request.AssessmentFilterRequest
import io.correcta.assessment.models.request.BulkUpdateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.request.ChangeAssessmentParametersRequest
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
import io.correcta.assessment.service.AssessmentQuestionService
import io.correcta.assessment.service.AssessmentQuestionTestcaseService
import io.correcta.assessment.service.AssessmentService
import io.correcta.assessment.service.AssessmentSessionService
import io.correcta.assessment.support.mock
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.mock.web.MockMultipartFile
import java.math.BigDecimal

/** The controllers only delegate: each test checks that every endpoint calls its service method. */
class ControllersTest {

  private val assessmentService = mock<AssessmentService>()
  private val questionService = mock<AssessmentQuestionService>()
  private val testcaseService = mock<AssessmentQuestionTestcaseService>()
  private val sessionService = mock<AssessmentSessionService>()

  private val pagination = PaginationRequest()
  private val sessionFilter = SessionFilterRequest()
  private val timeChange = SessionTimeChangeRequest(10)

  @Test
  fun `assessments controller delegates to the assessment service`() {
    val controller = AssessmentsControllerImpl(assessmentService)
    val filter = AssessmentFilterRequest()
    val create = CreateAssessmentRequest(title = "Lista", cohortId = "cohort-1")
    val update = UpdateAssessmentRequest(title = "Lista")
    val parameters = ChangeAssessmentParametersRequest()

    controller.getMyAssessments(filter, pagination)
    controller.getAssessment("a")
    controller.getAssessmentBySlug("c", "s")
    controller.createAssessment(create)
    controller.publishAssessment("a")
    controller.archiveAssessment("a")
    controller.editAssessment("a", update)
    controller.changeAssessmentParameters("a", parameters)

    assertSame(assessmentService, controller.assessmentService)
    verify(assessmentService).getMyAssessments(filter, pagination)
    verify(assessmentService).getAssessment("a")
    verify(assessmentService).getAssessmentBySlug("c", "s")
    verify(assessmentService).createAssessment(create)
    verify(assessmentService).publishAssessment("a")
    verify(assessmentService).archiveAssessment("a")
    verify(assessmentService).editAssessment("a", update)
    verify(assessmentService).changeAssessmentParameters("a", parameters)
  }

  @Test
  fun `question controllers delegate to the question service`() {
    val controller = AssessmentQuestionsControllerImpl(questionService)
    val internal = InternalAssessmentQuestionsControllerImpl(questionService)
    val create = CreateAssessmentQuestionRequest(title = "Q", statement = "S", maximumGrade = BigDecimal.ONE)
    val update = UpdateAssessmentQuestionRequest(position = 0, title = "Q", statement = "S", maximumGrade = BigDecimal.ONE)
    val types = listOf(AssessmentQuestionTestcaseType.PUBLIC)

    controller.getQuestions("a")
    controller.getQuestion("a", "q")
    controller.createQuestion("a", create)
    controller.editQuestion("a", "q", update)
    controller.deleteQuestion("a", "q")
    internal.getQuestion("a", "q", ProgrammingLanguage.C, types)

    assertSame(questionService, controller.assessmentQuestionService)
    assertSame(questionService, internal.assessmentQuestionService)
    verify(questionService).getQuestions("a")
    verify(questionService).getQuestion("a", "q")
    verify(questionService).createQuestion("a", create)
    verify(questionService).updateQuestion("a", "q", update)
    verify(questionService).deleteQuestion("a", "q")
    verify(questionService).getInternalQuestion("a", "q", ProgrammingLanguage.C, types)
  }

  @Test
  fun `test case controller delegates to the test case service`() {
    val controller = AssessmentQuestionTestcasesControllerImpl(testcaseService)
    val create = CreateAssessmentQuestionTestcaseRequest(expectedOutput = "1", type = AssessmentQuestionTestcaseType.PUBLIC)
    val update = UpdateAssessmentQuestionTestcaseRequest(position = 1, expectedOutput = "1", type = AssessmentQuestionTestcaseType.PUBLIC)
    val bulk = BulkUpdateAssessmentQuestionTestcaseRequest(listOf("t"), null, null, null)
    val import = ImportAssessmentQuestionTestcasesRequest(type = AssessmentQuestionTestcaseType.PUBLIC)
    val inputs = MockMultipartFile("inputs", ByteArray(0))
    val outputs = MockMultipartFile("outputs", ByteArray(0))

    controller.getTestcases("a", "q")
    controller.createTestcase("a", "q", create)
    controller.editTestcase("a", "q", "t", update)
    controller.bulkEditTestcases("a", "q", bulk)
    controller.deleteTestcase("a", "q", "t")
    controller.bulkDeleteTestcases("a", "q", listOf("t"))
    controller.importTestcases("a", "q", import, inputs, outputs)
    controller.reorderTestcases("a", "q", listOf("t"))

    assertSame(testcaseService, controller.assessmentQuestionTestcaseService)
    verify(testcaseService).getTestcases("a", "q")
    verify(testcaseService).createTestcase("a", "q", create)
    verify(testcaseService).updateTestcase("a", "q", "t", update)
    verify(testcaseService).bulkUpdateTestcases("a", "q", bulk)
    verify(testcaseService).deleteTestcase("a", "q", "t")
    verify(testcaseService).bulkDeleteTestcases("a", "q", listOf("t"))
    verify(testcaseService).importTestcases("a", "q", import, inputs, outputs)
    verify(testcaseService).reorderTestcases("a", "q", listOf("t"))
  }

  @Test
  fun `session controllers delegate to the session service`() {
    val controller = AssessmentSessionsControllerImpl(sessionService)
    val mine = MyAssessmentSessionsControllerImpl(sessionService)
    val internal = InternalAssessmentSessionsControllerImpl(sessionService)

    controller.getMySession("a")
    controller.heartbeat("a")
    controller.getSessions("a", sessionFilter, pagination)
    controller.countSessions("a")
    controller.getSession("a", "s")
    controller.getSessionEvents("a", "s")
    controller.reopenSession("a", "s", timeChange)
    controller.extendSession("a", "s", timeChange)
    controller.closeSession("a", "s")
    mine.getMySessions(sessionFilter, pagination)
    internal.getSubmissionStatus("a", "student")

    assertSame(sessionService, controller.assessmentSessionService)
    assertSame(sessionService, mine.assessmentSessionService)
    assertSame(sessionService, internal.assessmentSessionService)
    verify(sessionService).getMySession("a")
    verify(sessionService).heartbeat("a")
    verify(sessionService).getSessions("a", sessionFilter, pagination)
    verify(sessionService).countSessions("a")
    verify(sessionService).getSession("a", "s")
    verify(sessionService).getSessionEvents("a", "s")
    verify(sessionService).reopenSession("a", "s", timeChange)
    verify(sessionService).extendSession("a", "s", timeChange)
    verify(sessionService).closeSession("a", "s")
    verify(sessionService).getMySessions(sessionFilter, pagination)
    verify(sessionService).getSubmissionStatus("a", "student")
  }
}