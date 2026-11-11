package io.correcta.assessment.service

import io.correcta.assessment.entities.AssessmentQuestionTestcase
import io.correcta.assessment.entities.AssessmentQuestionTestcaseType.EVALUATION
import io.correcta.assessment.entities.AssessmentQuestionTestcaseType.PRIVATE
import io.correcta.assessment.entities.AssessmentQuestionTestcaseType.PUBLIC
import io.correcta.assessment.exceptions.BadRequestException
import io.correcta.assessment.exceptions.ConflictException
import io.correcta.assessment.exceptions.NotFoundException
import io.correcta.assessment.models.request.BulkUpdateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.request.CreateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.request.ImportAssessmentQuestionTestcasesRequest
import io.correcta.assessment.models.request.UpdateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.repositories.AssessmentQuestionRepository
import io.correcta.assessment.repositories.AssessmentQuestionTestcaseRepository
import io.correcta.assessment.repositories.AssessmentSessionRepository
import io.correcta.assessment.support.Fixtures.NOW
import io.correcta.assessment.support.Fixtures.assessment
import io.correcta.assessment.support.Fixtures.membership
import io.correcta.assessment.support.Fixtures.question
import io.correcta.assessment.support.Fixtures.testcase
import io.correcta.assessment.support.anyValue
import io.correcta.assessment.support.mock
import io.correcta.assessment.support.thenReturnFirstArgument
import io.correcta.assessment.support.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.verify
import org.springframework.mock.web.MockMultipartFile

class AssessmentQuestionTestcaseServiceTest {

  private val testcaseRepository = mock<AssessmentQuestionTestcaseRepository>()
  private val questionRepository = mock<AssessmentQuestionRepository>()
  private val accessService = mock<AssessmentAccessService>()
  private val sessionRepository = mock<AssessmentSessionRepository>()

  private val service = AssessmentQuestionTestcaseService(testcaseRepository, questionRepository, accessService, sessionRepository)

  private val assessment = assessment()
  private val question = question(assessment)
  private val assessmentId = assessment.id!!
  private val questionId = question.id!!

  private val public = testcase(question, id = "testcase-1", position = 1, type = PUBLIC)
  private val private = testcase(question, id = "testcase-2", position = 2, type = PRIVATE)
  private val evaluation = testcase(question, id = "testcase-3", position = 3, type = EVALUATION)

  private var nextId = 0

  @BeforeEach
  fun setUp() {
    whenever(accessService.findAssessmentWithVisibleQuestions(assessmentId)).thenReturn(assessment)
    whenever(accessService.findManageableAssessment(assessmentId)).thenReturn(assessment)
    whenever(accessService.requireCohortMember("cohort-1")).thenReturn(membership(role = "FACULTY"))
    whenever(questionRepository.findByIdAndAssessmentId(questionId, assessmentId)).thenReturn(question)
    whenever(testcaseRepository.findByIdAndQuestionId(public.id!!, questionId)).thenReturn(public)
    whenever(testcaseRepository.findByIdAndQuestionId(private.id!!, questionId)).thenReturn(private)
    whenever(testcaseRepository.findByQuestionIdOrderByPositionAsc(questionId)).thenReturn(listOf(public, private, evaluation))
    whenever(testcaseRepository.save(anyValue<AssessmentQuestionTestcase>())).thenReturnFirstArgument { it.fill() }
    whenever(testcaseRepository.saveAll(anyValue<List<AssessmentQuestionTestcase>>())).thenReturnFirstArgument { list -> list.forEach { it.fill() } }
  }

  private fun AssessmentQuestionTestcase.fill() {
    id = id ?: "testcase-new-${nextId++}"
    createdAt = NOW
    updatedAt = NOW
  }

  private fun missingQuestionCalls(): List<() -> Any> = listOf(
    { service.getTestcases(assessmentId, "missing") },
    { service.createTestcase(assessmentId, "missing", CreateAssessmentQuestionTestcaseRequest(expectedOutput = "1", type = PUBLIC)) },
    { service.updateTestcase(assessmentId, "missing", "testcase-1", updateRequest()) },
    { service.bulkUpdateTestcases(assessmentId, "missing", BulkUpdateAssessmentQuestionTestcaseRequest(listOf("testcase-1"), null, null, null)) },
    { service.deleteTestcase(assessmentId, "missing", "testcase-1") },
    { service.bulkDeleteTestcases(assessmentId, "missing", listOf("testcase-1")) },
    { service.importTestcases(assessmentId, "missing", ImportAssessmentQuestionTestcasesRequest(type = PUBLIC), file(""), file("")) },
    { service.reorderTestcases(assessmentId, "missing", emptyList()) },
  )

  private fun updateRequest(position: Int? = 1, expectedOutput: String? = "4", type: io.correcta.assessment.entities.AssessmentQuestionTestcaseType? = PRIVATE) = UpdateAssessmentQuestionTestcaseRequest(
    position = position,
    input = "2 2",
    expectedOutput = expectedOutput,
    timeLimitSeconds = 2,
    memoryLimitMB = 64,
    type = type,
  )

  private fun file(content: String) = MockMultipartFile("file", content.toByteArray())

  @Test
  fun `exposes its dependencies`() {
    assertSame(testcaseRepository, service.assessmentQuestionTestcaseRepository)
    assertSame(questionRepository, service.assessmentQuestionRepository)
    assertSame(accessService, service.assessmentAccessService)
    assertSame(sessionRepository, service.assessmentSessionRepository)
  }

  @Test
  fun `every operation on an unknown question is not found`() {
    missingQuestionCalls().forEach { call -> assertThrows<NotFoundException> { call() } }
  }

  @Test
  fun `faculty list every test case and students the visible ones`() {
    whenever(accessService.requireCohortMember("cohort-1")).thenReturn(membership(role = "FACULTY"))
    assertEquals(3, service.getTestcases(assessmentId, questionId).size)

    whenever(accessService.requireCohortMember("cohort-1")).thenReturn(membership(role = "STUDENT"))
    val visible = service.getTestcases(assessmentId, questionId)
    assertEquals(listOf("testcase-1", "testcase-2"), visible.map { it.id })
    assertEquals(listOf("3", null), visible.map { it.expectedOutput })
  }

  @Test
  fun `creates test cases at a free position or after the last one`() {
    val atPosition = service.createTestcase(assessmentId, questionId, CreateAssessmentQuestionTestcaseRequest(position = 7, input = "x", expectedOutput = "y", type = PRIVATE))
    assertEquals(7, atPosition.position)
    assertEquals(PRIVATE, atPosition.type)

    assertEquals(1, service.createTestcase(assessmentId, questionId, CreateAssessmentQuestionTestcaseRequest(expectedOutput = "y", type = PUBLIC)).position)

    whenever(testcaseRepository.findMaxPositionByQuestionId(questionId)).thenReturn(3)
    assertEquals(4, service.createTestcase(assessmentId, questionId, CreateAssessmentQuestionTestcaseRequest(expectedOutput = "y", type = PUBLIC)).position)
  }

  @Test
  fun `creating at a taken position conflicts`() {
    whenever(testcaseRepository.findByQuestionIdAndPosition(questionId, 1)).thenReturn(public)

    assertThrows<ConflictException> {
      service.createTestcase(assessmentId, questionId, CreateAssessmentQuestionTestcaseRequest(position = 1, expectedOutput = "y", type = PUBLIC))
    }
  }

  @Test
  fun `updates every field of a test case`() {
    val updated = service.updateTestcase(assessmentId, questionId, public.id!!, updateRequest(position = 5))

    assertEquals(5, updated.position)
    assertEquals("2 2", updated.input)
    assertEquals("4", updated.expectedOutput)
    assertEquals(2, updated.timeLimitSeconds)
    assertEquals(64, updated.memoryLimitMB)
    assertEquals(PRIVATE, updated.type)
  }

  @Test
  fun `null and unchanged fields keep their value`() {
    val samePosition = service.updateTestcase(assessmentId, questionId, public.id!!, updateRequest(position = 1, expectedOutput = null, type = null))
    assertEquals(1, samePosition.position)
    assertEquals("3", samePosition.expectedOutput)
    assertEquals(PUBLIC, samePosition.type)

    assertEquals(1, service.updateTestcase(assessmentId, questionId, public.id!!, updateRequest(position = null)).position)
  }

  @Test
  fun `updating to a taken position or an unknown test case fails`() {
    whenever(testcaseRepository.findByQuestionIdAndPosition(questionId, 2)).thenReturn(private)

    assertThrows<ConflictException> { service.updateTestcase(assessmentId, questionId, public.id!!, updateRequest(position = 2)) }
    assertThrows<NotFoundException> { service.updateTestcase(assessmentId, questionId, "missing", updateRequest()) }
  }

  @Test
  fun `bulk edits apply only the given fields`() {
    val ids = listOf(public.id!!, private.id!!)
    whenever(testcaseRepository.findByQuestionIdAndIdInOrderByPositionAsc(questionId, ids)).thenReturn(listOf(public, private))

    val changed = service.bulkUpdateTestcases(assessmentId, questionId, BulkUpdateAssessmentQuestionTestcaseRequest(ids, 10, 512, EVALUATION))
    assertEquals(listOf(10, 10), changed.map { it.timeLimitSeconds })
    assertEquals(listOf(512, 512), changed.map { it.memoryLimitMB })
    assertEquals(listOf(EVALUATION, EVALUATION), changed.map { it.type })

    val kept = service.bulkUpdateTestcases(assessmentId, questionId, BulkUpdateAssessmentQuestionTestcaseRequest(ids, null, null, null))
    assertEquals(listOf(10, 10), kept.map { it.timeLimitSeconds })
  }

  @Test
  fun `bulk edits of unknown test cases are not found`() {
    assertThrows<NotFoundException> {
      service.bulkUpdateTestcases(assessmentId, questionId, BulkUpdateAssessmentQuestionTestcaseRequest(listOf("missing"), null, null, null))
    }
  }

  @Test
  fun `deletes test cases, keeping grading test cases once students start`() {
    service.deleteTestcase(assessmentId, questionId, private.id!!)
    verify(testcaseRepository).delete(private)

    whenever(sessionRepository.existsByAssessmentId(assessmentId)).thenReturn(true)
    service.deleteTestcase(assessmentId, questionId, public.id!!)
    verify(testcaseRepository).delete(public)

    assertThrows<ConflictException> { service.deleteTestcase(assessmentId, questionId, private.id!!) }
    assertThrows<NotFoundException> { service.deleteTestcase(assessmentId, questionId, "missing") }
  }

  @Test
  fun `bulk deletes need known test cases`() {
    val ids = listOf(public.id!!, private.id!!)
    whenever(testcaseRepository.findByQuestionIdAndIdInOrderByPositionAsc(questionId, ids)).thenReturn(listOf(public, private))

    service.bulkDeleteTestcases(assessmentId, questionId, ids)
    verify(testcaseRepository).deleteAll(listOf(public, private))

    assertThrows<BadRequestException> { service.bulkDeleteTestcases(assessmentId, questionId, emptyList()) }
    assertThrows<NotFoundException> { service.bulkDeleteTestcases(assessmentId, questionId, listOf("missing")) }
  }

  @Test
  fun `imports one test case per line pair after the last test case`() {
    whenever(testcaseRepository.findMaxPositionByQuestionId(questionId)).thenReturn(3)

    val imported = service.importTestcases(
      assessmentId,
      questionId,
      ImportAssessmentQuestionTestcasesRequest(timeLimitSeconds = 3, memoryLimitMB = 128, type = EVALUATION),
      file("1 1\n2 2\n"),
      file("2\n4\n"),
    )

    assertEquals(listOf(4, 5), imported.map { it.position })
    assertEquals(listOf("2 2", "4"), listOf(imported[1].input, imported[1].expectedOutput))
    assertEquals(listOf(3, 128), listOf(imported[0].timeLimitSeconds, imported[0].memoryLimitMB))
  }

  @Test
  fun `imports need as many outputs as inputs`() {
    assertThrows<BadRequestException> {
      service.importTestcases(assessmentId, questionId, ImportAssessmentQuestionTestcasesRequest(type = PUBLIC), file("1\n2\n"), file("1\n"))
    }
  }

  @Test
  fun `reorders every test case from position 1`() {
    whenever(testcaseRepository.saveAllAndFlush(anyValue<List<AssessmentQuestionTestcase>>())).thenReturnFirstArgument {
      assertEquals(listOf(-1, -2, -3), it.map(AssessmentQuestionTestcase::position))
    }

    val reordered = service.reorderTestcases(assessmentId, questionId, listOf("testcase-3", "testcase-1", "testcase-2"))

    assertEquals(listOf("testcase-3", "testcase-1", "testcase-2"), reordered.map { it.id })
    assertEquals(listOf(1, 2, 3), reordered.map { it.position })
  }

  @Test
  fun `the new order must list exactly the existing test cases`() {
    assertThrows<BadRequestException> { service.reorderTestcases(assessmentId, questionId, listOf("testcase-1")) }
    assertThrows<BadRequestException> { service.reorderTestcases(assessmentId, questionId, listOf("testcase-1", "testcase-2", "other")) }
  }
}