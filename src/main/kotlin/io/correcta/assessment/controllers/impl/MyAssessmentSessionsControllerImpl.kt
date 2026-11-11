package io.correcta.assessment.controllers.impl

import io.correcta.assessment.controllers.MyAssessmentSessionsController
import io.correcta.assessment.models.request.PaginationRequest
import io.correcta.assessment.models.request.SessionFilterRequest
import io.correcta.assessment.models.response.GetAssessmentSessionResponse
import io.correcta.assessment.models.response.PagedResponse
import io.correcta.assessment.service.AssessmentSessionService
import org.springframework.web.bind.annotation.RestController

/** Delegates [MyAssessmentSessionsController] to [AssessmentSessionService]. */
@RestController
class MyAssessmentSessionsControllerImpl(
  val assessmentSessionService: AssessmentSessionService,
) : MyAssessmentSessionsController {

  override fun getMySessions(
    filter: SessionFilterRequest,
    pagination: PaginationRequest,
  ): PagedResponse<GetAssessmentSessionResponse> = assessmentSessionService.getMySessions(filter, pagination)
}