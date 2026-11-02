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


@Service
class AssessmentAccessService(
    val assessmentRepository: AssessmentRepository,
    val scopeEvaluator: ScopeEvaluatorComponent,
    val cohortMembershipService: CohortMembershipService,
    val assessmentSessionRepository: AssessmentSessionRepository,
    val entityManager: EntityManager,
) {


    fun findReadableAssessment(assessmentId: String): Assessment {
        return findReadableAssessmentWithMembership(assessmentId).first
    }


    fun findAssessmentWithVisibleQuestions(assessmentId: String): Assessment {
        val (assessment, membership) = findReadableAssessmentWithMembership(assessmentId)

        if (!membership.isFaculty() &&
            assessmentSessionRepository.findByAssessmentIdAndStudentProfileId(
                assessmentId,
                membership.profileId
            ) == null
        ) {
            throw ForbiddenException("Inicie a atividade para ver as questões.")
        }

        return assessment
    }


    fun findReadableAssessmentWithMembership(assessmentId: String): Pair<Assessment, CohortMembershipResponse> {
        val assessment = findAssessment(assessmentId)
        val membership = requireCohortMember(assessment.cohortId ?: throw assessmentNotFound())

        if (!membership.isFaculty() && assessment.state != AssessmentState.PUBLISHED) {
            throw assessmentNotFound()
        }

        return assessment to membership
    }


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
