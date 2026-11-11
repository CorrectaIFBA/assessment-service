package io.correcta.assessment.service

import io.correcta.assessment.entities.Assessment
import io.correcta.assessment.entities.AssessmentParameterChangeLog
import io.correcta.assessment.entities.AssessmentParameters
import io.correcta.assessment.entities.AssessmentState
import io.correcta.assessment.exceptions.BadRequestException
import io.correcta.assessment.exceptions.ConflictException
import io.correcta.assessment.exceptions.NotFoundException
import io.correcta.assessment.models.request.*
import io.correcta.assessment.models.response.GetAssessmentResponse
import io.correcta.assessment.models.response.PagedResponse
import io.correcta.assessment.repositories.*
import io.correcta.assessment.repositories.specifications.AssessmentSpecifications
import io.correcta.assessment.utils.PaginationExt.toPageRequest
import io.correcta.assessment.utils.PaginationExt.toPagedResponse
import io.correcta.assessment.utils.SlugExt.toSlug
import io.correcta.assessment.utils.SlugExt.uniqueSlug
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
  val assessmentSessionService: AssessmentSessionService,
) {

  @Transactional(readOnly = true)
  fun getMyAssessments(
    filter: AssessmentFilterRequest,
    pagination: PaginationRequest,
  ): PagedResponse<GetAssessmentResponse> {
    val pageRequest = pagination.toPageRequest(allowedSortFields = ALLOWED_SORT_FIELDS)

    val cohortId = filter.cohortId?.trim()?.takeIf { it.isNotEmpty() }
      ?: throw BadRequestException("A turma é obrigatória para listar atividades.")

    val membership = assessmentAccessService.requireCohortMember(cohortId)

    val specification = Specification.allOf(
      listOfNotNull(
        AssessmentSpecifications.isOfCohort(cohortId),
        AssessmentSpecifications.titleContains(filter.title),
        AssessmentSpecifications.hasState(filter.state),
        AssessmentSpecifications.hasState(AssessmentState.PUBLISHED).takeUnless { membership.isFaculty() },
      ),
    )

    val assessmentsPage = assessmentRepository.findAll(specification, pageRequest)

    val parametersByAssessmentId = assessmentParametersRepository
      .findAllByAssessmentIdIn(assessmentsPage.content.map { it.id!! })
      .associateBy { it.assessment.id!! }

    val assessmentIds = assessmentsPage.content.map { it.id!! }
    val questionsByAssessmentId = questionSummaries(assessmentIds)
    val scoresByAssessmentId = scores(assessmentIds)
    val sessionsByAssessmentId = assessmentSessionService.summarizeMySessions(assessmentIds)

    return assessmentsPage.toPagedResponse(pagination) { assessment ->
      GetAssessmentResponse.fromAssessment(
        assessment,
        parametersByAssessmentId[assessment.id],
        questionsByAssessmentId[assessment.id],
        scoresByAssessmentId[assessment.id],
        sessionsByAssessmentId[assessment.id],
      )
    }
  }

  @Transactional(readOnly = true)
  fun getAssessment(assessmentId: String): GetAssessmentResponse {
    val assessment = assessmentAccessService.findReadableAssessment(assessmentId)

    return respond(assessment, assessmentParametersRepository.findByAssessmentId(assessmentId))
  }

  @Transactional(readOnly = true)
  fun getAssessmentBySlug(cohortId: String, slug: String): GetAssessmentResponse {
    val assessment = assessmentRepository.findByCohortIdAndSlug(cohortId, slug)
      ?: throw NotFoundException("Atividade não encontrada.")

    return getAssessment(assessment.id!!)
  }

  @Transactional
  fun createAssessment(request: CreateAssessmentRequest): GetAssessmentResponse {
    val principal = assessmentAccessService.getAuthenticatedProfile()
    val cohortId = request.cohortId!!.trim()

    assessmentAccessService.requireActiveCohortFaculty(cohortId)

    val assessment = assessmentRepository.save(
      Assessment(
        ownerProfileId = principal.profileId,
        cohortId = cohortId,
        title = request.title.trim(),
        slug = request.slug?.also { requireSlugAvailable(cohortId, it, assessmentId = null) }
          ?: uniqueSlug(request.title.toSlug(FALLBACK_SLUG)) {
            assessmentRepository.existsByCohortIdAndSlug(
              cohortId,
              it,
            )
          },
        description = request.description.normalized(),
        dueDate = request.dueDate,
      ),
    )

    val parameters = assessmentParametersRepository.save(
      (request.parameters ?: AssessmentParametersRequest()).toAssessmentParameters(assessment),
    )

    return respond(assessment, parameters)
  }

  @Transactional
  fun publishAssessment(assessmentId: String): GetAssessmentResponse = changeState(assessmentId, AssessmentState.PUBLISHED, "A atividade já está publicada.")

  @Transactional
  fun archiveAssessment(assessmentId: String): GetAssessmentResponse = changeState(assessmentId, AssessmentState.ARCHIVED, "A atividade já está arquivada.")

  @Transactional
  fun editAssessment(assessmentId: String, request: UpdateAssessmentRequest): GetAssessmentResponse {
    val assessment = assessmentAccessService.findManageableAssessment(assessmentId)

    assessment.title = request.title.trim()
    request.slug?.takeIf { it != assessment.slug }?.let { slug ->
      requireSlugAvailable(assessment.cohortId!!, slug, assessmentId)
      assessment.slug = slug
    }
    assessment.description = request.description.normalized()
    assessment.dueDate = request.dueDate

    return respond(
      assessmentRepository.saveAndFlush(assessment),
      assessmentParametersRepository.findByAssessmentId(assessmentId),
    )
  }

  @Transactional
  fun changeAssessmentParameters(
    assessmentId: String,
    request: ChangeAssessmentParametersRequest,
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

  private fun changeState(
    assessmentId: String,
    targetState: AssessmentState,
    alreadyInStateMessage: String,
  ): GetAssessmentResponse {
    val assessment = assessmentAccessService.findManageableAssessment(assessmentId)

    if (assessment.state == targetState) {
      throw ConflictException(alreadyInStateMessage)
    }

    if (targetState == AssessmentState.PUBLISHED && !assessmentQuestionRepository.existsByAssessmentId(assessmentId)) {
      throw ConflictException("A atividade precisa de ao menos uma questão para ser publicada.")
    }

    assessment.state = targetState

    if (targetState == AssessmentState.ARCHIVED) {
      assessmentSessionService.finishSessionsOnArchive(assessment)
    }

    return respond(
      assessmentRepository.saveAndFlush(assessment),
      assessmentParametersRepository.findByAssessmentId(assessmentId),
    )
  }

  private fun respond(assessment: Assessment, parameters: AssessmentParameters?): GetAssessmentResponse {
    val assessmentIds = listOf(assessment.id!!)

    return GetAssessmentResponse.fromAssessment(
      assessment,
      parameters,
      questionSummaries(assessmentIds)[assessment.id],
      scores(assessmentIds)[assessment.id],
      assessmentSessionService.summarizeMySessions(assessmentIds)[assessment.id],
    )
  }

  private fun questionSummaries(assessmentIds: List<String>): Map<String, AssessmentQuestionRepository.QuestionSummary> {
    if (assessmentIds.isEmpty()) return emptyMap()

    return assessmentQuestionRepository.summarizeByAssessmentIds(assessmentIds).associateBy { it.assessmentId }
  }

  private fun scores(assessmentIds: List<String>): Map<String, BigDecimal?> {
    if (assessmentIds.isEmpty()) return emptyMap()

    return assessmentResultRepository
      .findByAssessmentIdInAndStudentProfileId(
        assessmentIds,
        assessmentAccessService.getAuthenticatedProfile().profileId,
      )
      .associate { it.assessment.id!! to it.finalScore }
  }

  private fun String?.normalized(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

  private fun requireSlugAvailable(cohortId: String, slug: String, assessmentId: String?) {
    val taken = if (assessmentId == null) {
      assessmentRepository.existsByCohortIdAndSlug(cohortId, slug)
    } else {
      assessmentRepository.existsByCohortIdAndSlugAndIdNot(cohortId, slug, assessmentId)
    }

    if (taken) throw ConflictException("Já existe uma atividade com o slug '$slug' nesta turma.")
  }

  private fun AssessmentParameters.snapshot(): Map<String, Any?> = mapOf(
    "canBeSubmittedAfterDueDate" to canBeSubmittedAfterDueDate,
    "maximumTimeAllowed" to maximumTimeAllowed,
    "showCompilationErrors" to showCompilationErrors,
    "canViewDefinitiveTestcaseScoringAfterSubmission" to canViewDefinitiveTestcaseScoringAfterSubmission,
  )

  companion object {
    private val ALLOWED_SORT_FIELDS = setOf("createdAt", "title", "dueDate")
    private const val FALLBACK_SLUG = "atividade"
  }
}