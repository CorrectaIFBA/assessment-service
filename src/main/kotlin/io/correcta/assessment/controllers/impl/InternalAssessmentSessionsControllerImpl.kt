package io.correcta.assessment.controllers.impl

import io.correcta.assessment.controllers.InternalAssessmentSessionsController
import io.correcta.assessment.models.response.GetSessionSubmissionStatusResponse
import io.correcta.assessment.service.AssessmentSessionService
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalAssessmentSessionsControllerImpl(
    val assessmentSessionService: AssessmentSessionService
) : InternalAssessmentSessionsController {

    override fun getSubmissionStatus(
        assessmentId: String,
        studentProfileId: String
    ): GetSessionSubmissionStatusResponse {
        return assessmentSessionService.getSubmissionStatus(assessmentId, studentProfileId)
    }
}
