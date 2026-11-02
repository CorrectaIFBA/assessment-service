package io.correcta.assessment.controllers.impl

import io.correcta.assessment.controllers.AssessmentQuestionsController
import io.correcta.assessment.models.request.CreateAssessmentQuestionRequest
import io.correcta.assessment.models.request.UpdateAssessmentQuestionRequest
import io.correcta.assessment.models.response.GetAssessmentQuestionResponse
import io.correcta.assessment.service.AssessmentQuestionService
import org.springframework.web.bind.annotation.RestController

@RestController
class AssessmentQuestionsControllerImpl(
    val assessmentQuestionService: AssessmentQuestionService
) : AssessmentQuestionsController {

    override fun getQuestions(assessmentId: String): List<GetAssessmentQuestionResponse> {
        return assessmentQuestionService.getQuestions(assessmentId)
    }

    override fun getQuestion(assessmentId: String, questionId: String): GetAssessmentQuestionResponse {
        return assessmentQuestionService.getQuestion(assessmentId, questionId)
    }

    override fun createQuestion(
        assessmentId: String,
        body: CreateAssessmentQuestionRequest
    ): GetAssessmentQuestionResponse {
        return assessmentQuestionService.createQuestion(assessmentId, body)
    }

    override fun editQuestion(
        assessmentId: String,
        questionId: String,
        body: UpdateAssessmentQuestionRequest
    ): GetAssessmentQuestionResponse {
        return assessmentQuestionService.updateQuestion(assessmentId, questionId, body)
    }

    override fun deleteQuestion(assessmentId: String, questionId: String) {
        assessmentQuestionService.deleteQuestion(assessmentId, questionId)
    }
}
