package io.correcta.assessment.service

import io.correcta.assessment.entities.Assessment
import io.correcta.assessment.entities.AssessmentState
import io.correcta.assessment.exceptions.ConflictException
import io.correcta.assessment.exceptions.ForbiddenException
import io.correcta.assessment.exceptions.InternalServerErrorException
import io.correcta.assessment.exceptions.NotFoundException
import io.correcta.assessment.models.response.clients.CohortMembershipResponse
import io.correcta.assessment.repositories.AssessmentRepository
import io.correcta.assessment.repositories.AssessmentSessionRepository
import io.correcta.assessment.security.components.ScopeEvaluatorComponent
import io.correcta.assessment.security.models.ProfilePrincipal
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.stereotype.Service

/** Loads assessments and checks that the caller may read or manage them, based on cohort membership. */
@Service
class AssessmentAccessService(
  val assessmentRepository: AssessmentRepository,
  val scopeEvaluator: ScopeEvaluatorComponent,
  val cohortMembershipService: CohortMembershipService,
  val assessmentSessionRepository: AssessmentSessionRepository,
  val entityManager: EntityManager,
) {

  /** Returns the assessment if the caller may read it. Students only read published assessments. */
  fun findReadableAssessment(assessmentId: String): Assessment = findReadableAssessmentWithMembership(assessmentId).first

  /**
   * Returns the assessment if the caller may see its questions: faculty always, students after they start a session.
   */
  fun findAssessmentWithVisibleQuestions(assessmentId: String): Assessment {
    val (assessment, membership) = findReadableAssessmentWithMembership(assessmentId)

    if (!membership.isFaculty() &&
      assessmentSessionRepository.findByAssessmentIdAndStudentProfileId(
        assessmentId,
        membership.profileId,
      ) == null
    ) {
      throw ForbiddenException("Inicie a atividade para ver as questões.")
    }

    return assessment
  }

  /** Returns the readable assessment and the caller membership in its cohort. */
  fun findReadableAssessmentWithMembership(assessmentId: String): Pair<Assessment, CohortMembershipResponse> {
    val assessment = findAssessment(assessmentId)
    val membership = requireCohortMember(assessment.cohortId ?: throw assessmentNotFound())

    if (!membership.isFaculty() && assessment.state != AssessmentState.PUBLISHED) {
      throw assessmentNotFound()
    }

    return assessment to membership
  }

  /** Returns the assessment, locked for update, if the caller is faculty of its active cohort. */
  fun findManageableAssessment(assessmentId: String): Assessment {
    val assessment = findAssessment(assessmentId)

    requireActiveCohortFaculty(assessment.cohortId ?: throw assessmentNotFound())
    entityManager.refresh(assessment, LockModeType.PESSIMISTIC_WRITE)

    return assessment
  }

  /** Requires the caller to be faculty (`403` otherwise) of an active cohort (`409` otherwise). */
  fun requireActiveCohortFaculty(cohortId: String): CohortMembershipResponse {
    val membership = cohortMembershipService.findCurrentMembership(cohortId, getAuthenticatedProfile().profileId)

    if (membership == null || !membership.isFaculty()) {
      throw ForbiddenException("Apenas docentes da turma podem gerenciar suas atividades.")
    }

    if (membership.cohortStatus != ACTIVE_COHORT_STATUS) {
      throw ConflictException("A turma não está ativa.")
    }

    return membership
  }

  /** Requires the caller to be a current member of the cohort (`403` otherwise). */
  fun requireCohortMember(cohortId: String): CohortMembershipResponse = cohortMembershipService.findCurrentMembership(cohortId, getAuthenticatedProfile().profileId)
    ?: throw ForbiddenException("Apenas membros da turma podem acessar atividades atribuídas a ela.")

  /**
   * Authenticated profile. Endpoints that call this method must use [io.correcta.assessment.security.annotations.IsLoggedUser].
   */
  fun getAuthenticatedProfile(): ProfilePrincipal = scopeEvaluator.currentProfile()
    ?: throw InternalServerErrorException("Falha ao recuperar o principal da autenticação")

  private fun findAssessment(assessmentId: String): Assessment = assessmentRepository.findById(assessmentId).orElseThrow { assessmentNotFound() }

  private fun assessmentNotFound() = NotFoundException("Atividade não encontrada.")

  companion object {
    private const val ACTIVE_COHORT_STATUS = "ACTIVE"
  }
}

/** Whether the membership has the `FACULTY` role. */
fun CohortMembershipResponse.isFaculty(): Boolean = role == "FACULTY"