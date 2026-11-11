package io.correcta.assessment.service

import io.correcta.assessment.entities.Assessment
import io.correcta.assessment.entities.AssessmentParameterChangeLog
import io.correcta.assessment.entities.AssessmentParameters
import io.correcta.assessment.entities.AssessmentResult
import io.correcta.assessment.entities.AssessmentState
import io.correcta.assessment.exceptions.BadRequestException
import io.correcta.assessment.exceptions.ConflictException
import io.correcta.assessment.exceptions.NotFoundException
import io.correcta.assessment.models.request.AssessmentFilterRequest
import io.correcta.assessment.models.request.AssessmentParametersRequest
import io.correcta.assessment.models.request.ChangeAssessmentParametersRequest
import io.correcta.assessment.models.request.CreateAssessmentRequest
import io.correcta.assessment.models.request.PaginationRequest
import io.correcta.assessment.models.request.UpdateAssessmentRequest
import io.correcta.assessment.models.response.GetAssessmentSessionSummaryResponse
import io.correcta.assessment.repositories.AssessmentParameterChangeLogRepository
import io.correcta.assessment.repositories.AssessmentParametersRepository
import io.correcta.assessment.repositories.AssessmentQuestionRepository
import io.correcta.assessment.repositories.AssessmentRepository
import io.correcta.assessment.repositories.AssessmentResultRepository
import io.correcta.assessment.support.Fixtures.NOW
import io.correcta.assessment.support.Fixtures.assessment
import io.correcta.assessment.support.Fixtures.membership
import io.correcta.assessment.support.Fixtures.parameters
import io.correcta.assessment.support.Fixtures.principal
import io.correcta.assessment.support.Fixtures.session
import io.correcta.assessment.support.anyValue
import io.correcta.assessment.support.mock
import io.correcta.assessment.support.thenReturnFirstArgument
import io.correcta.assessment.support.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import java.math.BigDecimal
import java.time.Instant

class AssessmentServiceTest {

  private val assessmentRepository = mock<AssessmentRepository>()
  private val parametersRepository = mock<AssessmentParametersRepository>()
  private val changeLogRepository = mock<AssessmentParameterChangeLogRepository>()
  private val questionRepository = mock<AssessmentQuestionRepository>()
  private val resultRepository = mock<AssessmentResultRepository>()
  private val accessService = mock<AssessmentAccessService>()
  private val sessionService = mock<AssessmentSessionService>()

  private val service = AssessmentService(
    assessmentRepository,
    parametersRepository,
    changeLogRepository,
    questionRepository,
    resultRepository,
    accessService,
    sessionService,
  )

  private val assessment = assessment()
  private val assessmentId = assessment.id!!

  private class Summary(
    override val assessmentId: String,
    override val questionCount: Long,
    override val totalGrade: BigDecimal,
  ) : AssessmentQuestionRepository.QuestionSummary

  @BeforeEach
  fun setUp() {
    whenever(accessService.getAuthenticatedProfile()).thenReturn(principal("student-a"))
    whenever(accessService.findReadableAssessment(assessmentId)).thenReturn(assessment)
    whenever(accessService.findManageableAssessment(assessmentId)).thenReturn(assessment)
    whenever(assessmentRepository.save(anyValue<Assessment>())).thenReturnFirstArgument {
      it.id = "assessment-new"
      it.createdAt = NOW
      it.updatedAt = NOW
    }
    whenever(assessmentRepository.saveAndFlush(anyValue<Assessment>())).thenReturnFirstArgument()
    whenever(parametersRepository.save(anyValue<AssessmentParameters>())).thenReturnFirstArgument { it.fill() }
    whenever(parametersRepository.saveAndFlush(anyValue<AssessmentParameters>())).thenReturnFirstArgument { it.fill() }
  }

  private fun AssessmentParameters.fill() {
    id = id ?: "parameters-new"
    createdAt = NOW
    updatedAt = NOW
  }

  private fun createRequest(slug: String? = null, description: String? = null, parameters: AssessmentParametersRequest? = null) = CreateAssessmentRequest(
    title = " Lista 1 ",
    slug = slug,
    description = description,
    cohortId = " cohort-1 ",
    dueDate = NOW,
    parameters = parameters,
  )

  @Test
  fun `exposes its dependencies`() {
    assertSame(assessmentRepository, service.assessmentRepository)
    assertSame(parametersRepository, service.assessmentParametersRepository)
    assertSame(changeLogRepository, service.assessmentParameterChangeLogRepository)
    assertSame(questionRepository, service.assessmentQuestionRepository)
    assertSame(resultRepository, service.assessmentResultRepository)
    assertSame(accessService, service.assessmentAccessService)
    assertSame(sessionService, service.assessmentSessionService)
  }

  @Test
  fun `lists the assessments of a cohort with their totals, score and session`() {
    whenever(accessService.requireCohortMember("cohort-1")).thenReturn(membership(role = "STUDENT"))
    whenever(assessmentRepository.findAll(anyValue<Specification<Assessment>>(), anyValue<Pageable>()))
      .thenReturn(PageImpl(listOf(assessment), PageRequest.of(0, 20), 1))
    whenever(parametersRepository.findAllByAssessmentIdIn(listOf(assessmentId))).thenReturn(listOf(parameters(assessment)))
    whenever(questionRepository.summarizeByAssessmentIds(listOf(assessmentId))).thenReturn(listOf(Summary(assessmentId, 2, BigDecimal("5"))))
    whenever(resultRepository.findByAssessmentIdInAndStudentProfileId(listOf(assessmentId), "student-a"))
      .thenReturn(listOf(AssessmentResult(assessment = assessment, studentProfileId = "student-a", finalScore = BigDecimal("4"))))
    whenever(sessionService.summarizeMySessions(listOf(assessmentId)))
      .thenReturn(mapOf(assessmentId to GetAssessmentSessionSummaryResponse.fromSession(session(assessment), NOW)))

    val page = service.getMyAssessments(AssessmentFilterRequest(cohortId = " cohort-1 ", title = "lista"), PaginationRequest(sortBy = "title"))

    val item = page.content.single()
    assertEquals(1, page.totalElements)
    assertEquals("title", page.sortBy)
    assertEquals("parameters-1", item.parameters!!.id)
    assertEquals(2, item.questionCount)
    assertEquals(BigDecimal("5"), item.totalGrade)
    assertEquals(BigDecimal("4"), item.score)
    assertEquals("session-1", item.mySession!!.id)
  }

  @Test
  fun `an empty page skips the totals`() {
    whenever(accessService.requireCohortMember("cohort-1")).thenReturn(membership(role = "FACULTY"))
    whenever(assessmentRepository.findAll(anyValue<Specification<Assessment>>(), anyValue<Pageable>())).thenReturn(PageImpl(emptyList()))

    val page = service.getMyAssessments(AssessmentFilterRequest(cohortId = "cohort-1", state = AssessmentState.DRAFT), PaginationRequest())

    assertTrue(page.content.isEmpty())
    verify(questionRepository, never()).summarizeByAssessmentIds(emptyList())
  }

  @Test
  fun `listing needs a cohort`() {
    assertThrows<BadRequestException> { service.getMyAssessments(AssessmentFilterRequest(), PaginationRequest()) }
    assertThrows<BadRequestException> { service.getMyAssessments(AssessmentFilterRequest(cohortId = "  "), PaginationRequest()) }
  }

  @Test
  fun `reads an assessment by id or by slug`() {
    whenever(assessmentRepository.findByCohortIdAndSlug("cohort-1", "lista-1")).thenReturn(assessment)

    assertEquals(assessmentId, service.getAssessment(assessmentId).id)
    assertEquals(assessmentId, service.getAssessmentBySlug("cohort-1", "lista-1").id)
    assertThrows<NotFoundException> { service.getAssessmentBySlug("cohort-1", "missing") }
  }

  @Test
  fun `creates a draft with a generated slug and default parameters`() {
    whenever(accessService.getAuthenticatedProfile()).thenReturn(principal("faculty-a"))
    whenever(assessmentRepository.existsByCohortIdAndSlug("cohort-1", "lista-1")).thenReturn(true)

    val created = service.createAssessment(createRequest(description = "  "))

    verify(accessService).requireActiveCohortFaculty("cohort-1")
    assertEquals("faculty-a", created.ownerProfileId)
    assertEquals("Lista 1", created.title)
    assertEquals("lista-1-2", created.slug)
    assertNull(created.description)
    assertEquals(AssessmentState.DRAFT, created.state)
    assertEquals(NOW.toString(), created.dueDate)
    assertFalse(created.parameters!!.canBeSubmittedAfterDueDate)
  }

  @Test
  fun `creates with the given slug, description and parameters`() {
    val created = service.createAssessment(
      createRequest(slug = "prova", description = " Prova final ", parameters = AssessmentParametersRequest(canBeSubmittedAfterDueDate = true, maximumTimeAllowed = 60)),
    )

    assertEquals("prova", created.slug)
    assertEquals("Prova final", created.description)
    assertEquals(60, created.parameters!!.maximumTimeAllowed)
  }

  @Test
  fun `a given slug must be free in the cohort`() {
    whenever(assessmentRepository.existsByCohortIdAndSlug("cohort-1", "prova")).thenReturn(true)

    assertThrows<ConflictException> { service.createAssessment(createRequest(slug = "prova")) }
  }

  @Test
  fun `publishing needs a question and archiving finishes the sessions`() {
    assertThrows<ConflictException> { service.publishAssessment(assessmentId) }

    whenever(questionRepository.existsByAssessmentId(assessmentId)).thenReturn(true)
    assertEquals(AssessmentState.PUBLISHED, service.publishAssessment(assessmentId).state)
    assertThrows<ConflictException> { service.publishAssessment(assessmentId) }

    assertEquals(AssessmentState.ARCHIVED, service.archiveAssessment(assessmentId).state)
    verify(sessionService).finishSessionsOnArchive(assessment)
    assertThrows<ConflictException> { service.archiveAssessment(assessmentId) }
  }

  @Test
  fun `edits the title, slug, description and due date`() {
    val edited = service.editAssessment(assessmentId, UpdateAssessmentRequest(title = " Lista 2 ", slug = "lista-2", description = " Nova ", dueDate = null))

    assertEquals("Lista 2", edited.title)
    assertEquals("lista-2", edited.slug)
    assertEquals("Nova", edited.description)
    assertNull(edited.dueDate)

    assertEquals("lista-2", service.editAssessment(assessmentId, UpdateAssessmentRequest(title = "Lista 2")).slug)
    assertEquals("lista-2", service.editAssessment(assessmentId, UpdateAssessmentRequest(title = "Lista 2", slug = "lista-2")).slug)
  }

  @Test
  fun `editing to a taken slug conflicts`() {
    whenever(assessmentRepository.existsByCohortIdAndSlugAndIdNot("cohort-1", "outra", assessmentId)).thenReturn(true)

    assertThrows<ConflictException> { service.editAssessment(assessmentId, UpdateAssessmentRequest(title = "Lista", slug = "outra")) }
  }

  @Test
  @Suppress("UNCHECKED_CAST")
  fun `changing parameters logs each changed value`() {
    whenever(accessService.getAuthenticatedProfile()).thenReturn(principal("faculty-a"))
    whenever(parametersRepository.findByAssessmentId(assessmentId)).thenReturn(parameters(assessment, maximumTimeAllowed = 30))

    val changed = service.changeAssessmentParameters(assessmentId, ChangeAssessmentParametersRequest(maximumTimeAllowed = 0, showCompilationErrors = false))

    assertNull(changed.parameters!!.maximumTimeAllowed)
    val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<AssessmentParameterChangeLog>>
    verify(changeLogRepository).saveAll(captor.capture())
    assertEquals(
      listOf(Triple("maximumTimeAllowed", "30", "null"), Triple("showCompilationErrors", "true", "false")),
      captor.value.map { Triple(it.parameterName, it.oldValue, it.newValue) },
    )
    assertTrue(captor.value.all { it.changerProfileId == "faculty-a" && it.assessment === assessment })
  }

  @Test
  fun `unchanged parameters are not saved again`() {
    whenever(parametersRepository.findByAssessmentId(assessmentId)).thenReturn(parameters(assessment))

    service.changeAssessmentParameters(assessmentId, ChangeAssessmentParametersRequest())

    verify(parametersRepository, never()).saveAndFlush(anyValue<AssessmentParameters>())
  }

  @Test
  fun `missing parameters are created with the defaults`() {
    val changed = service.changeAssessmentParameters(assessmentId, ChangeAssessmentParametersRequest())

    assertEquals("parameters-new", changed.parameters!!.id)
  }

  @Test
  fun `a due date in the past is kept as given`() {
    val dueDate = Instant.parse("2020-01-01T00:00:00Z")

    assertEquals(dueDate.toString(), service.editAssessment(assessmentId, UpdateAssessmentRequest(title = "Lista", dueDate = dueDate)).dueDate)
  }
}