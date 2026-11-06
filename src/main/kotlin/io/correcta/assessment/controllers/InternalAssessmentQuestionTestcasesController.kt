package io.correcta.assessment.controllers

import io.correcta.assessment.entities.AssessmentQuestionTestcaseType
import io.correcta.assessment.models.response.GetAssessmentQuestionTestcaseResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam


@RequestMapping("/api/v1/internal/assessments/{assessmentId}/questions/{questionId}/testcases")
@Tag(name = "Internal", description = "Service-to-service endpoints, not exposed through the gateway.")
interface InternalAssessmentQuestionTestcasesController {

    @GetMapping
    fun getTestcases(
        @PathVariable assessmentId: String,
        @PathVariable questionId: String,
        @RequestParam(required = false) types: List<AssessmentQuestionTestcaseType>?
    ): List<GetAssessmentQuestionTestcaseResponse>
}
