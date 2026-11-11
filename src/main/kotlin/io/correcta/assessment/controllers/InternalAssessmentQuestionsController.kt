package io.correcta.assessment.controllers

import io.correcta.assessment.entities.AssessmentQuestionTestcaseType
import io.correcta.assessment.entities.ProgrammingLanguage
import io.correcta.assessment.models.response.GetInternalAssessmentQuestionResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RequestMapping("/api/v1/internal/assessments/{assessmentId}/questions")
@Tag(name = "Internal", description = "Service-to-service endpoints, not exposed through the gateway.")
interface InternalAssessmentQuestionsController {

  @GetMapping("/{questionId}")
  fun getQuestion(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
    @RequestParam language: ProgrammingLanguage,
    @RequestParam(required = false) types: List<AssessmentQuestionTestcaseType>?,
  ): GetInternalAssessmentQuestionResponse
}