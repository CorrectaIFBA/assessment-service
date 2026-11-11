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
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/** Test cases of a question. Faculty of the cohort manage them; students only see the visible ones. */
@RequestMapping("/api/v1/assessments/{assessmentId}/questions/{questionId}/testcases")
@Tag(name = "Assessment Question Testcases", description = "Test cases a question's answers are run against.")
interface AssessmentQuestionTestcasesController {

  /**
   * Lists the test cases of the question, ordered by position. Students do not see `EVALUATION` test cases.
   */
  @GetMapping
  @IsLoggedUser
  fun getTestcases(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
  ): List<GetAssessmentQuestionTestcaseResponse>

  /** Creates a test case. Faculty only. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @IsLoggedUser
  fun createTestcase(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
    @Valid @RequestBody body: CreateAssessmentQuestionTestcaseRequest,
  ): GetAssessmentQuestionTestcaseResponse

  /** Replaces the fields of one test case. Faculty only. */
  @PostMapping("/{testcaseId}/edit")
  @IsLoggedUser
  fun editTestcase(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
    @PathVariable testcaseId: String,
    @Valid @RequestBody body: UpdateAssessmentQuestionTestcaseRequest,
  ): GetAssessmentQuestionTestcaseResponse

  /**
   * Changes the limits and the type of several test cases at once. Null fields keep their value. Faculty only.
   */
  @PostMapping("/bulk-edit")
  @IsLoggedUser
  fun bulkEditTestcases(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
    @Valid @RequestBody body: BulkUpdateAssessmentQuestionTestcaseRequest,
  ): List<GetAssessmentQuestionTestcaseResponse>

  /** Deletes one test case. Faculty only. */
  @DeleteMapping("/{testcaseId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @IsLoggedUser
  fun deleteTestcase(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
    @PathVariable testcaseId: String,
  )

  /** Deletes the test cases with the IDs in the body. Faculty only. */
  @PostMapping("/bulk-delete")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @IsLoggedUser
  fun bulkDeleteTestcases(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
    @RequestBody body: List<String>,
  )

  /**
   * Creates one test case per line of the `inputs` and `outputs` files, appended after the last test case. Faculty only.
   */
  @PostMapping(
    "/import",
    consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
  )
  @IsLoggedUser
  fun importTestcases(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
    @Valid @RequestPart("body") body: ImportAssessmentQuestionTestcasesRequest,
    @RequestPart("inputs") inputs: MultipartFile,
    @RequestPart("outputs") outputs: MultipartFile,
  ): List<GetAssessmentQuestionTestcaseResponse>

  /** Sets the order of all test cases of the question to the order of the IDs in the body. Faculty only. */
  @PostMapping("/reorder")
  @IsLoggedUser
  fun reorderTestcases(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
    @Valid @RequestBody body: List<String>,
  ): List<GetAssessmentQuestionTestcaseResponse>
}