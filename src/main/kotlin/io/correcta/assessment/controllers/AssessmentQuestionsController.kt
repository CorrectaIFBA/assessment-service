package io.correcta.assessment.controllers

import io.correcta.assessment.models.request.CreateAssessmentQuestionRequest
import io.correcta.assessment.models.request.ImportAssessmentQuestionTestcasesRequest
import io.correcta.assessment.models.request.UpdateAssessmentQuestionRequest
import io.correcta.assessment.models.response.GetAssessmentQuestionResponse
import io.correcta.assessment.models.response.GetAssessmentQuestionTestcaseResponse
import io.correcta.assessment.security.annotations.IsLoggedUser
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.multipart.MultipartFile

@RequestMapping("/api/v1/assessments/{assessmentId}/questions")
@Tag(name = "Assessment Questions", description = "Questions of an assessment, each with the languages it allows and their draft code.")
interface AssessmentQuestionsController {

    /**
     * lists the questions of an assessment ordered by position.
     * students can only see questions of published assessments in the cohorts they are enrolled in, once they started the assessment
     * while faculty can see questions of assessments in the cohorts they are teaching, regardless of the state of the assessment
     */
    @GetMapping
    @IsLoggedUser
    fun getQuestions(@PathVariable assessmentId: String): List<GetAssessmentQuestionResponse>

    /**
     * retrieves a specific question of an assessment, following the same visibility rules as the listing.
     */
    @GetMapping("/{questionId}")
    @IsLoggedUser
    fun getQuestion(@PathVariable assessmentId: String, @PathVariable questionId: String): GetAssessmentQuestionResponse

    /**
     * allows a teacher to add a question to an assessment in a cohort they teach.
     * must input a statement, a maximum grade and one or more allowed languages (C, PYTHON or JAVA), each with its optional draft code.
     * appended after the last question when no position is given.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @IsLoggedUser
    fun createQuestion(
        @PathVariable assessmentId: String,
        @Valid @RequestBody body: CreateAssessmentQuestionRequest
    ): GetAssessmentQuestionResponse

    /**
     * allows a teacher to edit a question of an assessment in a cohort they teach.
     */
    @PostMapping("/{questionId}/edit")
    @IsLoggedUser
    fun editQuestion(
        @PathVariable assessmentId: String,
        @PathVariable questionId: String,
        @Valid @RequestBody body: UpdateAssessmentQuestionRequest
    ): GetAssessmentQuestionResponse

    /**
     * allows a teacher to remove a question of an assessment in a cohort they teach.
     * the last question of a published assessment cannot be removed.
     */
    @DeleteMapping("/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @IsLoggedUser
    fun deleteQuestion(@PathVariable assessmentId: String, @PathVariable questionId: String)
}
