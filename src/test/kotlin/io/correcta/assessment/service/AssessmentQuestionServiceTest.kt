package io.correcta.assessment.service

import io.correcta.assessment.entities.AssessmentQuestion
import io.correcta.assessment.entities.AssessmentQuestionTestcaseType
import io.correcta.assessment.entities.AssessmentState
import io.correcta.assessment.entities.CodeDraft
import io.correcta.assessment.entities.ProgrammingLanguage
import io.correcta.assessment.exceptions.BadRequestException
import io.correcta.assessment.exceptions.ConflictException
import io.correcta.assessment.exceptions.NotFoundException
import io.correcta.assessment.models.request.CodeDraftRequest
import io.correcta.assessment.models.request.CreateAssessmentQuestionRequest
import io.correcta.assessment.models.request.UpdateAssessmentQuestionRequest
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.math.BigDecimal

class AssessmentQuestionServiceTest {

  private val questionRepository = mock<AssessmentQuestionRepository>()
  private val accessService = mock<AssessmentAccessService>()
  private val sessionRepository = mock<AssessmentSessionRepository>()
  private val testcaseRepository = mock<AssessmentQuestionTestcaseRepository>()

  private val service = AssessmentQuestionService(questionRepository, accessService, sessionRepository, testcaseRepository)

  private val assessment = assessment()
  private val question = question(assessment)
  private val assessmentId = assessment.id!!

  @BeforeEach
  fun setUp() {
    whenever(accessService.findAssessmentWithVisibleQuestions(assessmentId)).thenReturn(assessment)
    whenever(accessService.findManageableAssessment(assessmentId)).thenReturn(assessment)
    whenever(questionRepository.findByIdAndAssessmentId(question.id!!, assessmentId)).thenReturn(question)
    whenever(questionRepository.save(anyValue<AssessmentQuestion>())).thenReturnFirstArgument {
      it.id = "question-new"
      it.createdAt = NOW
      it.updatedAt = NOW
    }
    whenever(questionRepository.saveAndFlush(anyValue<AssessmentQuestion>())).thenReturnFirstArgument()
  }

  private fun createRequest(
    title: String = "Soma",
    position: Int? = null,
    slug: String? = null,
    codeDrafts: List<CodeDraftRequest> = listOf(CodeDraftRequest(ProgrammingLanguage.C, "code", "harness")),
  ) = CreateAssessmentQuestionRequest(
    position = position,
    title = "  $title  ",
    slug = slug,
    statement = " Enunciado ",
    codeDrafts = codeDrafts,
    maximumGrade = BigDecimal("2.5"),
    allowCopyPasting = false,
  )

  private fun updateRequest(
    position: Int = 0,
    slug: String? = null,
    maximumGrade: String = "2.5",
  ) = UpdateAssessmentQuestionRequest(
    position = position,
    title = " Soma nova ",
    slug = slug,
    statement = " Novo enunciado ",
    codeDrafts = listOf(CodeDraftRequest(ProgrammingLanguage.C, "new code", "new harness")),
    maximumGrade = BigDecimal(maximumGrade),
    allowCopyPasting = false,
  )

  @Test
  fun `exposes its dependencies`() {
    assertSame(questionRepository, service.assessmentQuestionRepository)
    assertSame(accessService, service.assessmentAccessService)
    assertSame(sessionRepository, service.assessmentSessionRepository)
    assertSame(testcaseRepository, service.assessmentQuestionTestcaseRepository)
  }

  @Test
  fun `lists and reads questions, with harnesses for faculty only`() {
    whenever(questionRepository.findByAssessmentIdOrderByPositionAsc(assessmentId)).thenReturn(listOf(question))
    whenever(accessService.requireCohortMember("cohort-1")).thenReturn(membership(role = "STUDENT"))

    assertNull(service.getQuestions(assessmentId).single().codeDrafts.single().harness)

    whenever(accessService.requireCohortMember("cohort-1")).thenReturn(membership(role = "FACULTY"))

    assertEquals("harness", service.getQuestion(assessmentId, question.id!!).codeDrafts.single().harness)
    assertThrows<NotFoundException> { service.getQuestion(assessmentId, "missing") }
  }

  @Test
  fun `internal reads return the harness of the language and the chosen test cases`() {
    val public = testcase(question)
    val evaluation = testcase(question, id = "testcase-2", position = 2, type = AssessmentQuestionTestcaseType.EVALUATION)
    whenever(testcaseRepository.findByQuestionIdOrderByPositionAsc(question.id!!)).thenReturn(listOf(public, evaluation))
    whenever(testcaseRepository.findByQuestionIdAndTypeInOrderByPositionAsc(question.id!!, listOf(AssessmentQuestionTestcaseType.EVALUATION)))
      .thenReturn(listOf(evaluation))

    val all = service.getInternalQuestion(assessmentId, question.id!!, ProgrammingLanguage.C, null)
    assertEquals("harness", all.harness)
    assertEquals(2, all.testcases.size)
    assertEquals(2, service.getInternalQuestion(assessmentId, question.id!!, ProgrammingLanguage.C, emptyList()).testcases.size)

    val filtered = service.getInternalQuestion(assessmentId, question.id!!, ProgrammingLanguage.C, listOf(AssessmentQuestionTestcaseType.EVALUATION))
    assertEquals(listOf("testcase-2"), filtered.testcases.map { it.id })
    assertEquals("1 2", filtered.testcases.single().input)
  }

  @Test
  fun `internal reads reject languages the question does not allow`() {
    // C is the only language today, so a draft without a language stands for a draft in another language.
    val otherLanguage = CodeDraft(ProgrammingLanguage.C)
    CodeDraft::class.java.getDeclaredField("language").apply { isAccessible = true }.set(otherLanguage, null)
    question.codeDrafts.add(0, otherLanguage)
    question.codeDrafts.removeAt(1)
    assertThrows<NotFoundException> { service.getInternalQuestion(assessmentId, question.id!!, ProgrammingLanguage.C, null) }

    question.codeDrafts.clear()

    assertThrows<NotFoundException> { service.getInternalQuestion(assessmentId, question.id!!, ProgrammingLanguage.C, null) }
  }

  @Test
  fun `creates questions after the last one with a generated slug`() {
    whenever(questionRepository.findTopByAssessmentIdOrderByPositionDesc(assessmentId)).thenReturn(question(assessment, position = 4))
    whenever(questionRepository.existsByAssessmentIdAndSlug(assessmentId, "soma")).thenReturn(true)

    val created = service.createQuestion(assessmentId, createRequest())

    assertEquals(5, created.position)
    assertEquals("soma-2", created.slug)
    assertEquals("Soma", created.title)
    assertEquals("Enunciado", created.statement)
    assertEquals("harness", created.codeDrafts.single().harness)
  }

  @Test
  fun `the first question goes to position 0 and never takes a reserved slug`() {
    whenever(questionRepository.existsByAssessmentIdAndSlug(assessmentId, "new-2")).thenReturn(true)

    val created = service.createQuestion(assessmentId, createRequest(title = "New"))

    assertEquals(0, created.position)
    assertEquals("new-3", created.slug)
  }

  @Test
  fun `a given slug must be free and not reserved`() {
    assertEquals("livre", service.createQuestion(assessmentId, createRequest(slug = "livre")).slug)

    assertThrows<BadRequestException> { service.createQuestion(assessmentId, createRequest(slug = "new")) }

    whenever(questionRepository.existsByAssessmentIdAndSlug(assessmentId, "soma")).thenReturn(true)
    assertThrows<ConflictException> { service.createQuestion(assessmentId, createRequest(slug = "soma")) }
  }

  @Test
  fun `a given position must be free`() {
    whenever(questionRepository.findByAssessmentIdAndPosition(assessmentId, 0)).thenReturn(question)

    assertThrows<ConflictException> { service.createQuestion(assessmentId, createRequest(position = 0)) }
  }

  @Test
  fun `each language has one code draft`() {
    val drafts = listOf(CodeDraftRequest(ProgrammingLanguage.C), CodeDraftRequest(ProgrammingLanguage.C))

    assertThrows<BadRequestException> { service.createQuestion(assessmentId, createRequest(codeDrafts = drafts)) }
  }

  @Test
  fun `updates every field of a question`() {
    whenever(questionRepository.findByAssessmentIdAndPosition(assessmentId, 0)).thenReturn(question)

    val updated = service.updateQuestion(assessmentId, question.id!!, updateRequest(slug = "soma-nova"))

    assertEquals("Soma nova", updated.title)
    assertEquals("soma-nova", updated.slug)
    assertEquals("Novo enunciado", updated.statement)
    assertEquals("new code", updated.codeDrafts.single().code)
    assertEquals(false, updated.allowCopyPasting)
  }

  @Test
  fun `keeps the slug when it is omitted or unchanged and rejects taken slugs`() {
    assertEquals("soma", service.updateQuestion(assessmentId, question.id!!, updateRequest()).slug)
    assertEquals("soma", service.updateQuestion(assessmentId, question.id!!, updateRequest(slug = "soma")).slug)
    verify(questionRepository, never()).existsByAssessmentIdAndSlugAndIdNot(assessmentId, "soma", question.id!!)

    whenever(questionRepository.existsByAssessmentIdAndSlugAndIdNot(assessmentId, "outra", question.id!!)).thenReturn(true)
    assertThrows<ConflictException> { service.updateQuestion(assessmentId, question.id!!, updateRequest(slug = "outra")) }
  }

  @Test
  fun `moving to a taken position conflicts`() {
    whenever(questionRepository.findByAssessmentIdAndPosition(assessmentId, 1)).thenReturn(question(assessment, id = "question-2", position = 1))

    assertThrows<ConflictException> { service.updateQuestion(assessmentId, question.id!!, updateRequest(position = 1)) }
  }

  @Test
  fun `the maximum grade only changes before students start`() {
    assertEquals(BigDecimal("5"), service.updateQuestion(assessmentId, question.id!!, updateRequest(maximumGrade = "5")).maximumGrade)

    whenever(sessionRepository.existsByAssessmentId(assessmentId)).thenReturn(true)
    assertThrows<ConflictException> { service.updateQuestion(assessmentId, question.id!!, updateRequest(maximumGrade = "3")) }
    assertEquals(BigDecimal("5.00"), service.updateQuestion(assessmentId, question.id!!, updateRequest(maximumGrade = "5.00")).maximumGrade)
  }

  @Test
  fun `updating an unknown question is not found`() {
    assertThrows<NotFoundException> { service.updateQuestion(assessmentId, "missing", updateRequest()) }
  }

  @Test
  fun `deletes questions of drafts and keeps one question in published assessments`() {
    service.deleteQuestion(assessmentId, question.id!!)
    verify(questionRepository).delete(question)

    assessment.state = AssessmentState.PUBLISHED
    whenever(questionRepository.countByAssessmentId(assessmentId)).thenReturn(1L)
    assertThrows<ConflictException> { service.deleteQuestion(assessmentId, question.id!!) }

    whenever(questionRepository.countByAssessmentId(assessmentId)).thenReturn(2L)
    service.deleteQuestion(assessmentId, question.id!!)
  }

  @Test
  fun `questions are not deleted after students start`() {
    whenever(sessionRepository.existsByAssessmentId(assessmentId)).thenReturn(true)

    assertThrows<ConflictException> { service.deleteQuestion(assessmentId, question.id!!) }
  }
}