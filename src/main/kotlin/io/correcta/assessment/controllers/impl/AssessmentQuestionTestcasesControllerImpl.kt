package io.correcta.assessment.controllers.impl

import io.correcta.assessment.controllers.AssessmentQuestionTestcasesController
import io.correcta.assessment.models.request.BulkUpdateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.request.CreateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.request.ImportAssessmentQuestionTestcasesRequest
import io.correcta.assessment.models.request.UpdateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.response.GetAssessmentQuestionTestcaseResponse
import io.correcta.assessment.service.AssessmentQuestionTestcaseService
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class AssessmentQuestionTestcasesControllerImpl(
    val assessmentQuestionTestcaseService: AssessmentQuestionTestcaseService
) : AssessmentQuestionTestcasesController {

    override fun getTestcases(assessmentId: String, questionId: String): List<GetAssessmentQuestionTestcaseResponse> {
        return assessmentQuestionTestcaseService.getTestcases(assessmentId, questionId)
    }

    override fun createTestcase(
        assessmentId: String,
        questionId: String,
        body: CreateAssessmentQuestionTestcaseRequest
    ): GetAssessmentQuestionTestcaseResponse {
        return assessmentQuestionTestcaseService.createTestcase(assessmentId, questionId, body)
    }

    override fun editTestcase(
        assessmentId: String,
        questionId: String,
        testcaseId: String,
        body: UpdateAssessmentQuestionTestcaseRequest
    ): GetAssessmentQuestionTestcaseResponse {
        return assessmentQuestionTestcaseService.updateTestcase(assessmentId, questionId, testcaseId, body)
    }

    override fun bulkEditTestcases(
        assessmentId: String,
        questionId: String,
        body: BulkUpdateAssessmentQuestionTestcaseRequest
    ): List<GetAssessmentQuestionTestcaseResponse> {
        return assessmentQuestionTestcaseService.bulkUpdateTestcases(assessmentId, questionId, body)
    }

    override fun deleteTestcase(assessmentId: String, questionId: String, testcaseId: String) {
        assessmentQuestionTestcaseService.deleteTestcase(assessmentId, questionId, testcaseId)
    }

    override fun bulkDeleteTestcases(assessmentId: String, questionId: String, body: List<String>) {
        assessmentQuestionTestcaseService.bulkDeleteTestcases(assessmentId, questionId, body)
    }

    override fun importTestcases(
        assessmentId: String,
        questionId: String,
        body: ImportAssessmentQuestionTestcasesRequest,
        inputs: MultipartFile,
        outputs: MultipartFile
    ): List<GetAssessmentQuestionTestcaseResponse> {
        return assessmentQuestionTestcaseService.importTestcases(assessmentId, questionId, body, inputs, outputs)
    }

    override fun reorderTestcases(
        assessmentId: String,
        questionId: String,
        body: List<String>
    ): List<GetAssessmentQuestionTestcaseResponse> {
        return assessmentQuestionTestcaseService.reorderTestcases(assessmentId, questionId, body)
    }
}
