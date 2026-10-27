package io.correcta.assessment.controllers

import io.correcta.assessment.models.request.BulkUpdateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.request.CreateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.request.ImportAssessmentQuestionTestcasesRequest
import io.correcta.assessment.models.request.UpdateAssessmentQuestionTestcaseRequest
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
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.multipart.MultipartFile

@RequestMapping("/api/v1/assessments/{assessmentId}/questions/{questionId}/testcases")
@Tag(name = "Assessment Question Testcases", description = "Test cases a question's answers are run against.")
interface AssessmentQuestionTestcasesController {

    /**
     * lists the test cases of a question ordered by position.
     * students only see PUBLIC and PRIVATE test cases, and only the input and expected output of PUBLIC ones,
     * while faculty see every test case with all of its values
     */
    @GetMapping
    @IsLoggedUser
    fun getTestcases(@PathVariable assessmentId: String, @PathVariable questionId: String): List<GetAssessmentQuestionTestcaseResponse>

    /**
     * allows a teacher to add a test case to a question of an assessment in a cohort they teach.
     * must input the expected output and the type, and optionally the input, the time limit and the memory limit.
     * appended after the last test case when no position is given.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @IsLoggedUser
    fun createTestcase(
        @PathVariable assessmentId: String,
        @PathVariable questionId: String,
        @Valid @RequestBody body: CreateAssessmentQuestionTestcaseRequest
    ): GetAssessmentQuestionTestcaseResponse

    /**
     * allows a teacher to edit a test case of a question of an assessment in a cohort they teach.
     */
    @PostMapping("/{testcaseId}/edit")
    @IsLoggedUser
    fun editTestcase(
        @PathVariable assessmentId: String,
        @PathVariable questionId: String,
        @PathVariable testcaseId: String,
        @Valid @RequestBody body: UpdateAssessmentQuestionTestcaseRequest
    ): GetAssessmentQuestionTestcaseResponse

    /**
     * allows a teacher to update multiple test cases of a question of an assessment in a cohort they teach.
     */
    @PostMapping("/bulk-edit")
    @IsLoggedUser
    fun bulkEditTestcases(
        @PathVariable assessmentId: String,
        @PathVariable questionId: String,
        @Valid @RequestBody body: BulkUpdateAssessmentQuestionTestcaseRequest
    ): List<GetAssessmentQuestionTestcaseResponse>

    /**
     * allows a teacher to remove a test case of a question of an assessment in a cohort they teach.
     */
    @DeleteMapping("/{testcaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @IsLoggedUser
    fun deleteTestcase(@PathVariable assessmentId: String, @PathVariable questionId: String, @PathVariable testcaseId: String)

    /**
     * allows a teacher to remove multiple test cases of a question of an assessment in a cohort they teach.
     */
    @PostMapping("/bulk-delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @IsLoggedUser
    fun bulkDeleteTestcases(
        @PathVariable assessmentId: String,
        @PathVariable questionId: String,
        @RequestBody body: List<String>
    )

    /**
     * allows a teacher to import test cases from inputs and outputs file to a question of an assessment in a cohort they teach.
     */
    @PostMapping(
        "/import",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    @IsLoggedUser
    fun importTestcases(
        @PathVariable assessmentId: String,
        @PathVariable questionId: String,
        @Valid @RequestPart("body") body: ImportAssessmentQuestionTestcasesRequest,
        @RequestPart("inputs") inputs: MultipartFile,
        @RequestPart("outputs") outputs: MultipartFile
    ): List<GetAssessmentQuestionTestcaseResponse>

    /**
     * allows a teacher to reorder a batch of test cases of a question of an assessment in a cohort they teach.
     */
    @PostMapping("/reorder")
    @IsLoggedUser
    fun reorderTestcases(
        @PathVariable assessmentId: String,
        @PathVariable questionId: String,
        @Valid @RequestBody body: List<String>
    ): List<GetAssessmentQuestionTestcaseResponse>
}
