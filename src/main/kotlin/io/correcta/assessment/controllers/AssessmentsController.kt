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
    fun editAssessment(
        @PathVariable assessmentId: String,
        @Valid @RequestBody body: UpdateAssessmentRequest
    ): GetAssessmentResponse


    @PatchMapping("/{assessmentId}/parameters")
    @IsLoggedUser
    fun changeAssessmentParameters(
        @PathVariable assessmentId: String,
        @Valid @RequestBody body: ChangeAssessmentParametersRequest
    ): GetAssessmentResponse
}
