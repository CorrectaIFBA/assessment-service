package io.correcta.assessment.controllers

import io.correcta.assessment.models.request.*
import io.correcta.assessment.models.response.GetAssessmentResponse
import io.correcta.assessment.models.response.PagedResponse
import io.correcta.assessment.security.annotations.IsLoggedUser
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/** Assessments (activities, lessons and exams) of the cohorts of the authenticated profile. */
@RequestMapping("/api/v1/assessments")
@Tag(name = "Assessments", description = "Activities, lessons and exams accessible by the authenticated profile.")
interface AssessmentsController {

  /** Lists the assessments of one cohort. Students only see published assessments. */
  @GetMapping
  @IsLoggedUser
  fun getMyAssessments(
    @Valid @ParameterObject filter: AssessmentFilterRequest,
    @Valid @ParameterObject pagination: PaginationRequest,
  ): PagedResponse<GetAssessmentResponse>

  /** Returns one assessment. */
  @GetMapping("/{assessmentId}")
  @IsLoggedUser
  fun getAssessment(@PathVariable assessmentId: String): GetAssessmentResponse

  /** Returns the assessment with the given slug in the cohort. */
  @GetMapping("/by-slug/{cohortId}/{slug}")
  @IsLoggedUser
  fun getAssessmentBySlug(@PathVariable cohortId: String, @PathVariable slug: String): GetAssessmentResponse

  /** Creates a draft assessment in a cohort. Active cohort faculty only. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @IsLoggedUser
  fun createAssessment(@Valid @RequestBody body: CreateAssessmentRequest): GetAssessmentResponse

  /** Publishes the assessment. It needs at least one question. Faculty only. */
  @PostMapping("/{assessmentId}/publish")
  @IsLoggedUser
  fun publishAssessment(@PathVariable assessmentId: String): GetAssessmentResponse

  /** Archives the assessment and finishes its running sessions. Faculty only. */
  @PostMapping("/{assessmentId}/archive")
  @IsLoggedUser
  fun archiveAssessment(@PathVariable assessmentId: String): GetAssessmentResponse

  /** Replaces the title, slug, description and due date. Faculty only. */
  @PostMapping("/{assessmentId}/edit")
  @IsLoggedUser
  fun editAssessment(
    @PathVariable assessmentId: String,
    @Valid @RequestBody body: UpdateAssessmentRequest,
  ): GetAssessmentResponse

  /** Changes the parameters of the assessment and logs every changed value. Faculty only. */
  @PatchMapping("/{assessmentId}/parameters")
  @IsLoggedUser
  fun changeAssessmentParameters(
    @PathVariable assessmentId: String,
    @Valid @RequestBody body: ChangeAssessmentParametersRequest,
  ): GetAssessmentResponse
}