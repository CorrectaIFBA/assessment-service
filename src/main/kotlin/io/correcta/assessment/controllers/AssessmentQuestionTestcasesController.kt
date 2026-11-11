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

@RequestMapping("/api/v1/assessments/{assessmentId}/questions/{questionId}/testcases")
@Tag(name = "Assessment Question Testcases", description = "Test cases a question's answers are run against.")
interface AssessmentQuestionTestcasesController {

  @GetMapping
  @IsLoggedUser
  fun getTestcases(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
  ): List<GetAssessmentQuestionTestcaseResponse>

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @IsLoggedUser
  fun createTestcase(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
    @Valid @RequestBody body: CreateAssessmentQuestionTestcaseRequest,
  ): GetAssessmentQuestionTestcaseResponse

  @PostMapping("/{testcaseId}/edit")
  @IsLoggedUser
  fun editTestcase(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
    @PathVariable testcaseId: String,
    @Valid @RequestBody body: UpdateAssessmentQuestionTestcaseRequest,
  ): GetAssessmentQuestionTestcaseResponse

  @PostMapping("/bulk-edit")
  @IsLoggedUser
  fun bulkEditTestcases(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
    @Valid @RequestBody body: BulkUpdateAssessmentQuestionTestcaseRequest,
  ): List<GetAssessmentQuestionTestcaseResponse>

  @DeleteMapping("/{testcaseId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @IsLoggedUser
  fun deleteTestcase(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
    @PathVariable testcaseId: String,
  )

  @PostMapping("/bulk-delete")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @IsLoggedUser
  fun bulkDeleteTestcases(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
    @RequestBody body: List<String>,
  )

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

  @PostMapping("/reorder")
  @IsLoggedUser
  fun reorderTestcases(
    @PathVariable assessmentId: String,
    @PathVariable questionId: String,
    @Valid @RequestBody body: List<String>,
  ): List<GetAssessmentQuestionTestcaseResponse>
}