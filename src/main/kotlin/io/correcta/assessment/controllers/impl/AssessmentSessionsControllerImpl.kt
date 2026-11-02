package io.correcta.assessment.controllers.impl

import io.correcta.assessment.controllers.AssessmentSessionsController
import io.correcta.assessment.entities.AssessmentSessionState
import io.correcta.assessment.models.request.PaginationRequest
import io.correcta.assessment.models.request.SessionFilterRequest
import io.correcta.assessment.models.request.SessionTimeChangeRequest
import io.correcta.assessment.models.response.GetAssessmentSessionEventResponse
import io.correcta.assessment.models.response.GetAssessmentSessionResponse
import io.correcta.assessment.models.response.PagedResponse
import io.correcta.assessment.service.AssessmentSessionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AssessmentSessionsControllerImpl(
    val assessmentSessionService: AssessmentSessionService
) : AssessmentSessionsController {

    override fun getMySession(assessmentId: String): GetAssessmentSessionResponse {
        return assessmentSessionService.getMySession(assessmentId)
    }

    override fun startSession(assessmentId: String): ResponseEntity<GetAssessmentSessionResponse> {
        val (session, created) = assessmentSessionService.startSession(assessmentId)

        return ResponseEntity.status(if (created) HttpStatus.CREATED else HttpStatus.OK).body(session)
    }

    override fun heartbeat(assessmentId: String): GetAssessmentSessionResponse {
        return assessmentSessionService.heartbeat(assessmentId)
    }

    override fun submitSession(assessmentId: String): GetAssessmentSessionResponse {
        return assessmentSessionService.submitSession(assessmentId)
    }

    override fun getSessions(
        assessmentId: String,
        filter: SessionFilterRequest,
        pagination: PaginationRequest
    ): PagedResponse<GetAssessmentSessionResponse> {
        return assessmentSessionService.getSessions(assessmentId, filter, pagination)
    }

    override fun countSessions(assessmentId: String): Map<AssessmentSessionState, Long> {
        return assessmentSessionService.countSessions(assessmentId)
    }

    override fun getSession(assessmentId: String, sessionId: String): GetAssessmentSessionResponse {
        return assessmentSessionService.getSession(assessmentId, sessionId)
    }

    override fun getSessionEvents(assessmentId: String, sessionId: String): List<GetAssessmentSessionEventResponse> {
        return assessmentSessionService.getSessionEvents(assessmentId, sessionId)
    }

    override fun reopenSession(assessmentId: String, sessionId: String, body: SessionTimeChangeRequest): GetAssessmentSessionResponse {
        return assessmentSessionService.reopenSession(assessmentId, sessionId, body)
    }

    override fun extendSession(assessmentId: String, sessionId: String, body: SessionTimeChangeRequest): GetAssessmentSessionResponse {
        return assessmentSessionService.extendSession(assessmentId, sessionId, body)
    }

    override fun closeSession(assessmentId: String, sessionId: String): GetAssessmentSessionResponse {
        return assessmentSessionService.closeSession(assessmentId, sessionId)
    }
}
