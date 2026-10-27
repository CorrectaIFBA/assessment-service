package io.correcta.assessment.controllers

import io.correcta.assessment.models.request.AssessmentFilterRequest
import io.correcta.assessment.models.request.ChangeAssessmentParametersRequest
import io.correcta.assessment.models.request.CreateAssessmentRequest
import io.correcta.assessment.models.request.PaginationRequest
import io.correcta.assessment.models.request.UpdateAssessmentRequest
import io.correcta.assessment.models.response.GetAssessmentResponse
import io.correcta.assessment.models.response.PagedResponse
import io.correcta.assessment.models.response.error.ErrorResponse
import io.correcta.assessment.models.response.error.ValidationErrorResponse
import io.correcta.assessment.security.annotations.IsLoggedUser
import io.swagger.v3.oas.annotations.Operation
import org.springdoc.core.annotations.ParameterObject
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

@RequestMapping("/api/v1/assessments")
@Tag(name = "Assessments", description = "Activities, lessons and exams accessible by the authenticated profile.")
interface AssessmentsController {

    /**
     * gathers the assessments the authenticated profile has access to, only those shared with the profile through a cohort
     * students can only see assessments in the cohorts they are enrolled in and are in published state
     * while faculty can see assessments in the cohorts they are teaching, regardless of the state of the assessment
     */
    @GetMapping
    @IsLoggedUser
    fun getMyAssessments(
        @Valid @ParameterObject filter: AssessmentFilterRequest,
        @Valid @ParameterObject pagination: PaginationRequest
    ): PagedResponse<GetAssessmentResponse>

    /**
     * retrieves a specific assessment by its ID, only if the authenticated profile has access to it
     * students can only see assessments in the cohorts they are enrolled in and are in published state
     * while faculty can see assessments in the cohorts they are teaching, regardless of the state of the assessment
     */
    @GetMapping("/{assessmentId}")
    @IsLoggedUser
    fun getAssessment(@PathVariable assessmentId: String): GetAssessmentResponse

    /*
     * allows a teacher to create a new assessment, mandatorily associated with a cohort they are teaching.
     * must input a title, and optionally a description and a due date. the assessment will be created in draft state.
     * must optionally provide parameters based on @AssessmentParameters
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @IsLoggedUser
    fun createAssessment(@Valid @RequestBody body: CreateAssessmentRequest): GetAssessmentResponse

    /**
     * allows a teacher to publish an assessment in a cohort they teach, making it available to students in the associated cohort.
     */
    @PostMapping("/{assessmentId}/publish")
    @IsLoggedUser
    fun publishAssessment(@PathVariable assessmentId: String): GetAssessmentResponse

    /**
     * allows a teacher to archive an assessment in a cohort they teach, making it unavailable to students in the associated cohort.
     */
    @PostMapping("/{assessmentId}/archive")
    @IsLoggedUser
    fun archiveAssessment(@PathVariable assessmentId: String): GetAssessmentResponse

    /**
     * allows a teacher to edit the properties of an assessment in a cohort they teach, such as title, description and due date.
     */
    @PostMapping("/{assessmentId}/edit")
    @IsLoggedUser
    fun editAssessment(@PathVariable assessmentId: String, @Valid @RequestBody body: UpdateAssessmentRequest): GetAssessmentResponse

    /**
     * allows a teacher to change a or many parameters of an assessment in a cohort they teach, such as canBeSubmittedAfterDueDate, needsToFinishInOneSitting, maximumTimeAllowed, monitorForCheating, canViewDefinitiveTestcaseScoringAfterSubmission.
     *
     */
    @PatchMapping("/{assessmentId}/parameters")
    @IsLoggedUser
    fun changeAssessmentParameters(@PathVariable assessmentId: String, @Valid @RequestBody body: ChangeAssessmentParametersRequest): GetAssessmentResponse
}
