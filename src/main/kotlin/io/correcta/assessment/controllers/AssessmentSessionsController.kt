package io.correcta.assessment.controllers

import io.correcta.assessment.entities.AssessmentSessionState
import io.correcta.assessment.models.request.PaginationRequest
import io.correcta.assessment.models.request.SessionFilterRequest
import io.correcta.assessment.models.request.SessionTimeChangeRequest
import io.correcta.assessment.models.response.GetAssessmentSessionEventResponse
import io.correcta.assessment.models.response.GetAssessmentSessionResponse
import io.correcta.assessment.models.response.PagedResponse
import io.correcta.assessment.security.annotations.IsLoggedUser
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Sessions of one assessment: the authenticated student's own session (`/me`) and the faculty view of all sessions.
 */
@RequestMapping("/api/v1/assessments/{assessmentId}/sessions")
@Tag(
  name = "Assessment Sessions",
  description = "A student's attempt at an assessment: start, heartbeats and submission.",
)
interface AssessmentSessionsController {

  /** Returns the session of the authenticated student. */
  @GetMapping("/me")
  @IsLoggedUser
  fun getMySession(@PathVariable assessmentId: String): GetAssessmentSessionResponse

  /** Starts the session of the authenticated student (`201`), or resumes the running one (`200`). */
  @PostMapping("/me/start")
  @IsLoggedUser
  fun startSession(@PathVariable assessmentId: String): ResponseEntity<GetAssessmentSessionResponse>

  /** Records that the coding page is still open and adds the elapsed time to the session. */
  @PostMapping("/me/heartbeat")
  @IsLoggedUser
  fun heartbeat(@PathVariable assessmentId: String): GetAssessmentSessionResponse

  /** Lists the sessions of the assessment. Faculty only. */
  @GetMapping
  @IsLoggedUser
  fun getSessions(
    @PathVariable assessmentId: String,
    @Valid @ParameterObject filter: SessionFilterRequest,
    @Valid @ParameterObject pagination: PaginationRequest,
  ): PagedResponse<GetAssessmentSessionResponse>

  /** Counts the sessions of the assessment per state. Faculty only. */
  @GetMapping("/summary")
  @IsLoggedUser
  fun countSessions(@PathVariable assessmentId: String): Map<AssessmentSessionState, Long>

  /** Returns one session of the assessment. Faculty only. */
  @GetMapping("/{sessionId}")
  @IsLoggedUser
  fun getSession(@PathVariable assessmentId: String, @PathVariable sessionId: String): GetAssessmentSessionResponse

  /** Lists the events of one session, oldest first. Faculty only. */
  @GetMapping("/{sessionId}/events")
  @IsLoggedUser
  fun getSessionEvents(
    @PathVariable assessmentId: String,
    @PathVariable sessionId: String,
  ): List<GetAssessmentSessionEventResponse>

  /** Reopens a finished session for the given extra minutes. Faculty only. */
  @PostMapping("/{sessionId}/reopen")
  @IsLoggedUser
  fun reopenSession(
    @PathVariable assessmentId: String,
    @PathVariable sessionId: String,
    @Valid @RequestBody body: SessionTimeChangeRequest,
  ): GetAssessmentSessionResponse

  /** Adds extra minutes to a running session that has a time limit. Faculty only. */
  @PostMapping("/{sessionId}/extend")
  @IsLoggedUser
  fun extendSession(
    @PathVariable assessmentId: String,
    @PathVariable sessionId: String,
    @Valid @RequestBody body: SessionTimeChangeRequest,
  ): GetAssessmentSessionResponse

  /** Finishes a running session as submitted. Faculty only. */
  @PostMapping("/{sessionId}/close")
  @IsLoggedUser
  fun closeSession(@PathVariable assessmentId: String, @PathVariable sessionId: String): GetAssessmentSessionResponse
}