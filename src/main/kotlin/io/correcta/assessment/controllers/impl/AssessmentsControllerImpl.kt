package io.correcta.assessment.controllers.impl

import io.correcta.assessment.controllers.AssessmentsController
import io.correcta.assessment.models.request.*
import io.correcta.assessment.models.response.GetAssessmentResponse
import io.correcta.assessment.models.response.PagedResponse
import io.correcta.assessment.service.AssessmentService
import org.springframework.web.bind.annotation.RestController

@RestController
class AssessmentsControllerImpl(
  val assessmentService: AssessmentService,
) : AssessmentsController {

  override fun getMyAssessments(
    filter: AssessmentFilterRequest,
    pagination: PaginationRequest,
  ): PagedResponse<GetAssessmentResponse> = assessmentService.getMyAssessments(filter, pagination)

  override fun getAssessment(assessmentId: String): GetAssessmentResponse = assessmentService.getAssessment(assessmentId)

  override fun getAssessmentBySlug(cohortId: String, slug: String): GetAssessmentResponse = assessmentService.getAssessmentBySlug(cohortId, slug)

  override fun createAssessment(body: CreateAssessmentRequest): GetAssessmentResponse = assessmentService.createAssessment(body)

  override fun publishAssessment(assessmentId: String): GetAssessmentResponse = assessmentService.publishAssessment(assessmentId)

  override fun archiveAssessment(assessmentId: String): GetAssessmentResponse = assessmentService.archiveAssessment(assessmentId)

  override fun editAssessment(assessmentId: String, body: UpdateAssessmentRequest): GetAssessmentResponse = assessmentService.editAssessment(assessmentId, body)

  override fun changeAssessmentParameters(
    assessmentId: String,
    body: ChangeAssessmentParametersRequest,
  ): GetAssessmentResponse = assessmentService.changeAssessmentParameters(assessmentId, body)
}