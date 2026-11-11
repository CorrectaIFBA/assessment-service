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

@RequestMapping("/api/v1/assessments/{assessmentId}/sessions")
@Tag(
  name = "Assessment Sessions",
  description = "A student's attempt at an assessment: start, heartbeats and submission.",
)
interface AssessmentSessionsController {

  @GetMapping("/me")
  @IsLoggedUser
  fun getMySession(@PathVariable assessmentId: String): GetAssessmentSessionResponse

  @PostMapping("/me/start")
  @IsLoggedUser
  fun startSession(@PathVariable assessmentId: String): ResponseEntity<GetAssessmentSessionResponse>

  @PostMapping("/me/heartbeat")
  @IsLoggedUser
  fun heartbeat(@PathVariable assessmentId: String): GetAssessmentSessionResponse

  @GetMapping
  @IsLoggedUser
  fun getSessions(
    @PathVariable assessmentId: String,
    @Valid @ParameterObject filter: SessionFilterRequest,
    @Valid @ParameterObject pagination: PaginationRequest,
  ): PagedResponse<GetAssessmentSessionResponse>

  @GetMapping("/summary")
  @IsLoggedUser
  fun countSessions(@PathVariable assessmentId: String): Map<AssessmentSessionState, Long>

  @GetMapping("/{sessionId}")
  @IsLoggedUser
  fun getSession(@PathVariable assessmentId: String, @PathVariable sessionId: String): GetAssessmentSessionResponse

  @GetMapping("/{sessionId}/events")
  @IsLoggedUser
  fun getSessionEvents(
    @PathVariable assessmentId: String,
    @PathVariable sessionId: String,
  ): List<GetAssessmentSessionEventResponse>

  @PostMapping("/{sessionId}/reopen")
  @IsLoggedUser
  fun reopenSession(
    @PathVariable assessmentId: String,
    @PathVariable sessionId: String,
    @Valid @RequestBody body: SessionTimeChangeRequest,
  ): GetAssessmentSessionResponse

  @PostMapping("/{sessionId}/extend")
  @IsLoggedUser
  fun extendSession(
    @PathVariable assessmentId: String,
    @PathVariable sessionId: String,
    @Valid @RequestBody body: SessionTimeChangeRequest,
  ): GetAssessmentSessionResponse

  @PostMapping("/{sessionId}/close")
  @IsLoggedUser
  fun closeSession(@PathVariable assessmentId: String, @PathVariable sessionId: String): GetAssessmentSessionResponse
}