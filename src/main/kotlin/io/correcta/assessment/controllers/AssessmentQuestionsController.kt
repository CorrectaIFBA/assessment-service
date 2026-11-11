package io.correcta.assessment.controllers

import io.correcta.assessment.models.request.CreateAssessmentQuestionRequest
import io.correcta.assessment.models.request.UpdateAssessmentQuestionRequest
import io.correcta.assessment.models.response.GetAssessmentQuestionResponse
import io.correcta.assessment.security.annotations.IsLoggedUser
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * Questions of an assessment. Faculty of the cohort manage them; students see them after they start the assessment.
 */
@RequestMapping("/api/v1/assessments/{assessmentId}/questions")
@Tag(
  name = "Assessment Questions",
  description = "Questions of an assessment, each with the languages it allows and their draft code.",
)
interface AssessmentQuestionsController {

  /** Lists the questions of the assessment, ordered by position. */
  @GetMapping
  @IsLoggedUser
  fun getQuestions(@PathVariable assessmentId: String): List<GetAssessmentQuestionResponse>

  /** Returns one question. The harness is only returned to faculty. */
  @GetMapping("/{questionId}")
  @IsLoggedUser
  fun getQuestion(@PathVariable assessmentId: String, @PathVariable questionId: String): GetAssessmentQuestionResponse

  /** Creates a question. Faculty only. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @IsLoggedUser
  fun createQuestion(
    @PathVariable assessmentId: String,
    @Valid @RequestBody body: CreateAssessmentQuestionRequest,
  ): GetAssessmentQuestionResponse

  /** Replaces the fields of a question. Faculty only. */
  @PostMapping("/{questionId}/edit")
  @IsLoggedUser
  fun editQuestion(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
    @Valid @RequestBody body: UpdateAssessmentQuestionRequest,
  ): GetAssessmentQuestionResponse

  /** Deletes a question. Faculty only, and only before any student starts the assessment. */
  @DeleteMapping("/{questionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @IsLoggedUser
  fun deleteQuestion(@PathVariable assessmentId: String, @PathVariable questionId: String)
}