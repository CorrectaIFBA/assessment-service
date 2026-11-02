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

    



    @GetMapping
    @IsLoggedUser
    fun getMyAssessments(
        @Valid @ParameterObject filter: AssessmentFilterRequest,
        @Valid @ParameterObject pagination: PaginationRequest
    ): PagedResponse<GetAssessmentResponse>

    



    @GetMapping("/{assessmentId}")
    @IsLoggedUser
    fun getAssessment(@PathVariable assessmentId: String): GetAssessmentResponse

    

    @GetMapping("/by-slug/{cohortId}/{slug}")
    @IsLoggedUser
    fun getAssessmentBySlug(@PathVariable cohortId: String, @PathVariable slug: String): GetAssessmentResponse

    



    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @IsLoggedUser
    fun createAssessment(@Valid @RequestBody body: CreateAssessmentRequest): GetAssessmentResponse

    

    @PostMapping("/{assessmentId}/publish")
    @IsLoggedUser
    fun publishAssessment(@PathVariable assessmentId: String): GetAssessmentResponse

    

    @PostMapping("/{assessmentId}/archive")
    @IsLoggedUser
    fun archiveAssessment(@PathVariable assessmentId: String): GetAssessmentResponse

    

    @PostMapping("/{assessmentId}/edit")
    @IsLoggedUser
    fun editAssessment(@PathVariable assessmentId: String, @Valid @RequestBody body: UpdateAssessmentRequest): GetAssessmentResponse

    


    @PatchMapping("/{assessmentId}/parameters")
    @IsLoggedUser
    fun changeAssessmentParameters(@PathVariable assessmentId: String, @Valid @RequestBody body: ChangeAssessmentParametersRequest): GetAssessmentResponse
}
