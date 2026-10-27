package io.correcta.assessment.service

import io.correcta.assessment.entities.Assessment
import io.correcta.assessment.entities.AssessmentParameterChangeLog
import io.correcta.assessment.entities.AssessmentParameters
import io.correcta.assessment.entities.AssessmentState
import io.correcta.assessment.exceptions.BadRequestException
import io.correcta.assessment.exceptions.ConflictException
import io.correcta.assessment.models.request.AssessmentFilterRequest
import io.correcta.assessment.models.request.AssessmentParametersRequest
import io.correcta.assessment.models.request.ChangeAssessmentParametersRequest
import io.correcta.assessment.models.request.CreateAssessmentRequest
import io.correcta.assessment.models.request.PaginationRequest
import io.correcta.assessment.models.request.UpdateAssessmentRequest
import io.correcta.assessment.models.response.GetAssessmentResponse
import io.correcta.assessment.models.response.PagedResponse
import io.correcta.assessment.repositories.AssessmentParameterChangeLogRepository
import io.correcta.assessment.repositories.AssessmentParametersRepository
import io.correcta.assessment.repositories.AssessmentQuestionRepository
import io.correcta.assessment.repositories.AssessmentRepository
import io.correcta.assessment.repositories.AssessmentResultRepository
import io.correcta.assessment.repositories.specifications.AssessmentSpecifications
import io.correcta.assessment.utils.PaginationExt.toPageRequest
import io.correcta.assessment.utils.PaginationExt.toPagedResponse
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class AssessmentService(
    val assessmentRepository: AssessmentRepository,
    val assessmentParametersRepository: AssessmentParametersRepository,
    val assessmentParameterChangeLogRepository: AssessmentParameterChangeLogRepository,
    val assessmentQuestionRepository: AssessmentQuestionRepository,
    val assessmentResultRepository: AssessmentResultRepository,
    val assessmentAccessService: AssessmentAccessService,
) {

    /**
     * gathers the assessments the authenticated profile has access to, only those shared with the profile through a cohort
     * students can only see assessments in the cohorts they are enrolled in and are in published state
     * while faculty can see assessments in the cohorts they are teaching, regardless of the state of the assessment
     */
    @Transactional(readOnly = true)
    fun getMyAssessments(
        filter: AssessmentFilterRequest,
        pagination: PaginationRequest
    ): PagedResponse<GetAssessmentResponse> {
        val pageRequest = pagination.toPageRequest(allowedSortFields = ALLOWED_SORT_FIELDS)

        // cohort-service only resolves memberships one cohort at a time, so listing is scoped to a single cohort
        val cohortId = filter.cohortId?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw BadRequestException("A turma é obrigatória para listar atividades.")

        val membership = assessmentAccessService.requireCohortMember(cohortId)

        // for students both state filters are combined, so asking for anything other than PUBLISHED yields an empty page
        val specification = Specification.allOf(
            listOfNotNull(
                AssessmentSpecifications.isOfCohort(cohortId),
                AssessmentSpecifications.titleContains(filter.title),
                AssessmentSpecifications.hasState(filter.state),
                AssessmentSpecifications.hasState(AssessmentState.PUBLISHED).takeUnless { membership.isFaculty() },
            )
        )

        val assessmentsPage = assessmentRepository.findAll(specification, pageRequest)

        val parametersByAssessmentId = assessmentParametersRepository
            .findAllByAssessmentIdIn(assessmentsPage.content.map { it.id!! })
            .associateBy { it.assessment.id!! }

        val assessmentIds = assessmentsPage.content.map { it.id!! }
        val questionsByAssessmentId = questionSummaries(assessmentIds)
        val scoresByAssessmentId = scores(assessmentIds)

        return assessmentsPage.toPagedResponse(pagination) { assessment ->
            GetAssessmentResponse.fromAssessment(
                assessment,
                parametersByAssessmentId[assessment.id],
                questionsByAssessmentId[assessment.id],
                scoresByAssessmentId[assessment.id],
            )
        }
    }

    /**
     * retrieves a specific assessment by its ID, only if the authenticated profile has access to it
     * students can only see assessments in the cohorts they are enrolled in and are in published state
     * while faculty can see assessments in the cohorts they are teaching, regardless of the state of the assessment
     */
    @Transactional(readOnly = true)
    fun getAssessment(assessmentId: String): GetAssessmentResponse {
        val assessment = assessmentAccessService.findReadableAssessment(assessmentId)

        return respond(assessment, assessmentParametersRepository.findByAssessmentId(assessmentId))
    }

    /*
     * allows a teacher to create a new assessment, mandatorily associated with a cohort they are teaching.
     * must input a title, and optionally a description and a due date. the assessment will be created in draft state.
     * must optionally provide parameters based on @AssessmentParameters
     */
    @Transactional
    fun createAssessment(request: CreateAssessmentRequest): GetAssessmentResponse {
        val principal = assessmentAccessService.getAuthenticatedProfile()
        // non-null is guaranteed by @NotBlank; nullable only so a missing field fails validation instead of deserialization
        val cohortId = request.cohortId!!.trim()

        assessmentAccessService.requireActiveCohortFaculty(cohortId)

        val assessment = assessmentRepository.save(
            Assessment(
                ownerProfileId = principal.profileId,
                cohortId = cohortId,
                title = request.title.trim(),
                description = request.description.normalized(),
                dueDate = request.dueDate,
            )
        )

        val parameters = assessmentParametersRepository.save(
            (request.parameters ?: AssessmentParametersRequest()).toAssessmentParameters(assessment)
        )

        return respond(assessment, parameters)
    }

    /**
     * allows a teacher to publish an assessment in a cohort they teach, making it available to students in the associated cohort.
     */
    @Transactional
    fun publishAssessment(assessmentId: String): GetAssessmentResponse {
        return changeState(assessmentId, AssessmentState.PUBLISHED, "A atividade já está publicada.")
    }

    /**
     * allows a teacher to archive an assessment in a cohort they teach, making it unavailable to students in the associated cohort.
     */
    @Transactional
    fun archiveAssessment(assessmentId: String): GetAssessmentResponse {
        return changeState(assessmentId, AssessmentState.ARCHIVED, "A atividade já está arquivada.")
    }

    /**
     * allows a teacher to edit the properties of an assessment in a cohort they teach, such as title, description and due date.
     */
    @Transactional
    fun editAssessment(assessmentId: String, request: UpdateAssessmentRequest): GetAssessmentResponse {
        val assessment = assessmentAccessService.findManageableAssessment(assessmentId)

        assessment.title = request.title.trim()
        assessment.description = request.description.normalized()
        assessment.dueDate = request.dueDate

        return respond(
            assessmentRepository.saveAndFlush(assessment),
            assessmentParametersRepository.findByAssessmentId(assessmentId)
        )
    }

    /**
     * allows a teacher to change a or many parameters of an assessment in a cohort they teach, such as canBeSubmittedAfterDueDate, needsToFinishInOneSitting, maximumTimeAllowed, monitorForCheating, canViewDefinitiveTestcaseScoringAfterSubmission.
     * should log the changes made to the parameters, including the previous and new values, the timestamp of the change, and the profile ID of the user who made the change.
     */
    @Transactional
    fun changeAssessmentParameters(
        assessmentId: String,
        request: ChangeAssessmentParametersRequest
    ): GetAssessmentResponse {
        val principal = assessmentAccessService.getAuthenticatedProfile()
        val assessment = assessmentAccessService.findManageableAssessment(assessmentId)

        val parameters = assessmentParametersRepository.findByAssessmentId(assessmentId)
            ?: AssessmentParametersRequest().toAssessmentParameters(assessment)

        val previousValues = parameters.snapshot()
        request.applyChangesTo(parameters)
        val currentValues = parameters.snapshot()

        val changeLogs = currentValues
            .filter { (name, value) -> previousValues[name] != value }
            .map { (name, value) ->
                AssessmentParameterChangeLog(
                    assessment = assessment,
                    changerProfileId = principal.profileId,
                    parameterName = name,
                    oldValue = previousValues[name].toString(),
                    newValue = value.toString(),
                )
            }

        if (changeLogs.isEmpty() && parameters.id != null) {
            return respond(assessment, parameters)
        }

        val savedParameters = assessmentParametersRepository.saveAndFlush(parameters)
        assessmentParameterChangeLogRepository.saveAll(changeLogs)

        return respond(assessment, savedParameters)
    }

    private fun changeState(assessmentId: String, targetState: AssessmentState, alreadyInStateMessage: String): GetAssessmentResponse {
        val assessment = assessmentAccessService.findManageableAssessment(assessmentId)

        if (assessment.state == targetState) {
            throw ConflictException(alreadyInStateMessage)
        }

        // the assessment row is locked, so no question can be removed between this check and the state change
        if (targetState == AssessmentState.PUBLISHED && !assessmentQuestionRepository.existsByAssessmentId(assessmentId)) {
            throw ConflictException("A atividade precisa de ao menos uma questão para ser publicada.")
        }

        assessment.state = targetState

        return respond(
            assessmentRepository.saveAndFlush(assessment),
            assessmentParametersRepository.findByAssessmentId(assessmentId)
        )
    }

    /**
     * the assessment as answered by every endpoint: its parameters, the totals of its questions
     * and, for a graded student, their score.
     */
    private fun respond(assessment: Assessment, parameters: AssessmentParameters?): GetAssessmentResponse {
        val assessmentIds = listOf(assessment.id!!)

        return GetAssessmentResponse.fromAssessment(
            assessment,
            parameters,
            questionSummaries(assessmentIds)[assessment.id],
            scores(assessmentIds)[assessment.id],
        )
    }

    private fun questionSummaries(assessmentIds: List<String>): Map<String, AssessmentQuestionRepository.QuestionSummary> {
        if (assessmentIds.isEmpty()) return emptyMap()

        return assessmentQuestionRepository.summarizeByAssessmentIds(assessmentIds).associateBy { it.assessmentId }
    }

    /**
     * results only exist for students, so for faculty this is always empty.
     */
    private fun scores(assessmentIds: List<String>): Map<String, BigDecimal?> {
        if (assessmentIds.isEmpty()) return emptyMap()

        return assessmentResultRepository
            .findByAssessmentIdInAndStudentProfileId(assessmentIds, assessmentAccessService.getAuthenticatedProfile().profileId)
            .associate { it.assessment.id!! to it.finalScore }
    }

    private fun String?.normalized(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * the values tracked by the parameter change log, keyed by parameter name.
     * a new parameter only needs an entry here to be audited.
     */
    private fun AssessmentParameters.snapshot(): Map<String, Any?> = mapOf(
        "canBeSubmittedAfterDueDate" to canBeSubmittedAfterDueDate,
        "needsToFinishInOneSitting" to needsToFinishInOneSitting,
        "maximumTimeAllowed" to maximumTimeAllowed,
        "monitorForCheating" to monitorForCheating,
        "canViewDefinitiveTestcaseScoringAfterSubmission" to canViewDefinitiveTestcaseScoringAfterSubmission,
    )

    companion object {
        private val ALLOWED_SORT_FIELDS = setOf("createdAt", "title", "dueDate")
    }
}
