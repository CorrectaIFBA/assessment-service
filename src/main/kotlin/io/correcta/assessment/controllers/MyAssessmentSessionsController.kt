package io.correcta.assessment.controllers

import io.correcta.assessment.models.request.PaginationRequest
import io.correcta.assessment.models.request.SessionFilterRequest
import io.correcta.assessment.models.response.GetAssessmentSessionResponse
import io.correcta.assessment.models.response.PagedResponse
import io.correcta.assessment.security.annotations.IsLoggedUser
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/api/v1/assessment-sessions")
@Tag(name = "My Assessment Sessions", description = "The authenticated student's sessions in every cohort.")
interface MyAssessmentSessionsController {

  @GetMapping("/me")
  @IsLoggedUser
  fun getMySessions(
    @Valid @ParameterObject filter: SessionFilterRequest,
    @Valid @ParameterObject pagination: PaginationRequest,
  ): PagedResponse<GetAssessmentSessionResponse>
}