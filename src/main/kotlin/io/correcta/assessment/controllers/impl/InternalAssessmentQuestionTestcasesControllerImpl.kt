package io.correcta.assessment.controllers.impl

import io.correcta.assessment.controllers.InternalAssessmentQuestionTestcasesController
import io.correcta.assessment.entities.AssessmentQuestionTestcaseType
import io.correcta.assessment.models.response.GetAssessmentQuestionTestcaseResponse
import io.correcta.assessment.service.AssessmentQuestionTestcaseService
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalAssessmentQuestionTestcasesControllerImpl(
    val assessmentQuestionTestcaseService: AssessmentQuestionTestcaseService
) : InternalAssessmentQuestionTestcasesController {

    override fun getTestcases(
        assessmentId: String,
        questionId: String,
        types: List<AssessmentQuestionTestcaseType>?
    ): List<GetAssessmentQuestionTestcaseResponse> {
        return assessmentQuestionTestcaseService.getInternalTestcases(assessmentId, questionId, types)
    }
}
