package io.correcta.assessment.controllers

import io.correcta.assessment.models.response.GetSessionSubmissionStatusResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/api/v1/internal/assessments/{assessmentId}/sessions")
@Tag(name = "Internal", description = "Service-to-service endpoints, not exposed through the gateway.")
interface InternalAssessmentSessionsController {

  @GetMapping("/{studentProfileId}")
  fun getSubmissionStatus(
    @PathVariable assessmentId: String,
    @PathVariable studentProfileId: String,
  ): GetSessionSubmissionStatusResponse
}