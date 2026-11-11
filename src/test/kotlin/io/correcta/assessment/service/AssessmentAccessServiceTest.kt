package io.correcta.assessment.service

import io.correcta.assessment.entities.AssessmentState
import io.correcta.assessment.exceptions.ConflictException
import io.correcta.assessment.exceptions.ForbiddenException
import io.correcta.assessment.exceptions.InternalServerErrorException
import io.correcta.assessment.exceptions.NotFoundException
import io.correcta.assessment.repositories.AssessmentRepository
import io.correcta.assessment.repositories.AssessmentSessionRepository
import io.correcta.assessment.security.components.ScopeEvaluatorComponent
import io.correcta.assessment.support.Fixtures.assessment
import io.correcta.assessment.support.Fixtures.membership
import io.correcta.assessment.support.Fixtures.principal
import io.correcta.assessment.support.Fixtures.session
import io.correcta.assessment.support.mock
import io.correcta.assessment.support.whenever
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.verify
import java.util.Optional

class AssessmentAccessServiceTest {

  private val assessmentRepository = mock<AssessmentRepository>()
  private val scopeEvaluator = mock<ScopeEvaluatorComponent>()
  private val cohortMembershipService = mock<CohortMembershipService>()
  private val assessmentSessionRepository = mock<AssessmentSessionRepository>()
  private val entityManager = mock<EntityManager>()

  private val service = AssessmentAccessService(
    assessmentRepository,
    scopeEvaluator,
    cohortMembershipService,
    assessmentSessionRepository,
    entityManager,
  )

  private val draft = assessment()
  private val published = assessment(id = "assessment-2", state = AssessmentState.PUBLISHED)

  @BeforeEach
  fun setUp() {
    whenever(assessmentRepository.findById(draft.id!!)).thenReturn(Optional.of(draft))
    whenever(assessmentRepository.findById(published.id!!)).thenReturn(Optional.of(published))
  }

  private fun signIn(profileId: String, role: String?, status: String = "ACTIVE") {
    whenever(scopeEvaluator.currentProfile()).thenReturn(principal(profileId))
    whenever(cohortMembershipService.findCurrentMembership("cohort-1", profileId))
      .thenReturn(role?.let { membership(role = it, status = status, profileId = profileId) })
  }

  @Test
  fun `exposes its dependencies`() {
    assertSame(assessmentRepository, service.assessmentRepository)
    assertSame(scopeEvaluator, service.scopeEvaluator)
    assertSame(cohortMembershipService, service.cohortMembershipService)
    assertSame(assessmentSessionRepository, service.assessmentSessionRepository)
    assertSame(entityManager, service.entityManager)
  }

  @Test
  fun `faculty read drafts and students only read published assessments`() {
    signIn("faculty-a", "FACULTY")
    assertSame(draft, service.findReadableAssessment(draft.id!!))

    signIn("student-a", "STUDENT")
    assertThrows<NotFoundException> { service.findReadableAssessment(draft.id!!) }
    assertSame(published, service.findReadableAssessmentWithMembership(published.id!!).first)
  }

  @Test
  fun `unknown assessments and assessments without cohort are not found`() {
    signIn("faculty-a", "FACULTY")
    whenever(assessmentRepository.findById("missing")).thenReturn(Optional.empty())
    whenever(assessmentRepository.findById("orphan")).thenReturn(Optional.of(assessment(id = "orphan", cohortId = null)))

    assertThrows<NotFoundException> { service.findReadableAssessment("missing") }
    assertThrows<NotFoundException> { service.findReadableAssessment("orphan") }
    assertThrows<NotFoundException> { service.findManageableAssessment("orphan") }
  }

  @Test
  fun `students see the questions only after they start`() {
    signIn("faculty-a", "FACULTY")
    assertSame(published, service.findAssessmentWithVisibleQuestions(published.id!!))

    signIn("student-a", "STUDENT")
    assertThrows<ForbiddenException> { service.findAssessmentWithVisibleQuestions(published.id!!) }

    whenever(assessmentSessionRepository.findByAssessmentIdAndStudentProfileId(published.id!!, "student-a"))
      .thenReturn(session(published))
    assertSame(published, service.findAssessmentWithVisibleQuestions(published.id!!))
  }

  @Test
  fun `managing locks the assessment for active faculty`() {
    signIn("faculty-a", "FACULTY")

    assertSame(draft, service.findManageableAssessment(draft.id!!))
    verify(entityManager).refresh(draft, LockModeType.PESSIMISTIC_WRITE)
  }

  @Test
  fun `only faculty of an active cohort manage it`() {
    signIn("outsider", null)
    assertThrows<ForbiddenException> { service.requireActiveCohortFaculty("cohort-1") }

    signIn("student-a", "STUDENT")
    assertThrows<ForbiddenException> { service.requireActiveCohortFaculty("cohort-1") }

    signIn("faculty-a", "FACULTY", status = "ARCHIVED")
    assertThrows<ConflictException> { service.requireActiveCohortFaculty("cohort-1") }

    signIn("faculty-a", "FACULTY")
    assertEquals("faculty-a", service.requireActiveCohortFaculty("cohort-1").profileId)
  }

  @Test
  fun `non-members are forbidden`() {
    signIn("outsider", null)

    assertThrows<ForbiddenException> { service.requireCohortMember("cohort-1") }
  }

  @Test
  fun `a missing principal is a server error`() {
    whenever(scopeEvaluator.currentProfile()).thenReturn(null)

    assertThrows<InternalServerErrorException> { service.getAuthenticatedProfile() }
  }

  @Test
  fun `faculty role is detected`() {
    assertTrue(membership(role = "FACULTY").isFaculty())
    assertFalse(membership(role = "STUDENT").isFaculty())
  }
}