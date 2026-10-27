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

/**
 * access rules shared by everything nested under an assessment.
 * every check is resolved against the assessment's cohort membership in cohort-service.
 */
@Service
class AssessmentAccessService(
    val assessmentRepository: AssessmentRepository,
    val scopeEvaluator: ScopeEvaluatorComponent,
    val cohortMembershipService: CohortMembershipService,
    val assessmentSessionRepository: AssessmentSessionRepository,
    val entityManager: EntityManager,
) {

    /**
     * students can only reach published assessments, faculty can reach any state.
     * unpublished assessments answer 404 to students, as if they did not exist.
     */
    fun findReadableAssessment(assessmentId: String): Assessment {
        return findReadableAssessmentWithMembership(assessmentId).first
    }

    /**
     * questions stay hidden from a student until they start the assessment, which opens their session;
     * faculty always see them.
     */
    fun findAssessmentWithVisibleQuestions(assessmentId: String): Assessment {
        val (assessment, membership) = findReadableAssessmentWithMembership(assessmentId)

        if (!membership.isFaculty() &&
            assessmentSessionRepository.findByAssessmentIdAndStudentProfileId(assessmentId, membership.profileId) == null
        ) {
            throw ForbiddenException("Inicie a atividade para ver as questões.")
        }

        return assessment
    }

    private fun findReadableAssessmentWithMembership(assessmentId: String): Pair<Assessment, CohortMembershipResponse> {
        val assessment = findAssessment(assessmentId)
        val membership = requireCohortMember(assessment.cohortId ?: throw assessmentNotFound())

        if (!membership.isFaculty() && assessment.state != AssessmentState.PUBLISHED) {
            throw assessmentNotFound()
        }

        return assessment to membership
    }

    /**
     * any faculty member of the assessment's active cohort may manage it, not only its owner.
     * the row is locked after authorization, so concurrent changes to the same assessment run one at a time
     * without holding the lock during the cohort-service call. requires an active transaction.
     */
    fun findManageableAssessment(assessmentId: String): Assessment {
        val assessment = findAssessment(assessmentId)

        requireActiveCohortFaculty(assessment.cohortId ?: throw assessmentNotFound())
        entityManager.refresh(assessment, LockModeType.PESSIMISTIC_WRITE)

        return assessment
    }

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

    /**
     * reading does not require an active cohort, so members keep access to assessments of archived cohorts.
     */
    fun requireCohortMember(cohortId: String): CohortMembershipResponse {
        return cohortMembershipService.findCurrentMembership(cohortId, getAuthenticatedProfile().profileId)
            ?: throw ForbiddenException("Apenas membros da turma podem acessar atividades atribuídas a ela.")
    }

    fun getAuthenticatedProfile(): ProfilePrincipal {
        return scopeEvaluator.currentProfile()
            ?: throw InternalServerErrorException("Falha ao recuperar o principal da autenticação")
    }

    private fun findAssessment(assessmentId: String): Assessment {
        return assessmentRepository.findById(assessmentId).orElseThrow { assessmentNotFound() }
    }

    private fun assessmentNotFound() = NotFoundException("Atividade não encontrada.")

    companion object {
        private const val ACTIVE_COHORT_STATUS = "ACTIVE"
    }
}

fun CohortMembershipResponse.isFaculty(): Boolean = role == "FACULTY"
