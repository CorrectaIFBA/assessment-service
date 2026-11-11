package io.correcta.assessment.controllers.impl

import io.correcta.assessment.controllers.InternalAssessmentQuestionsController
import io.correcta.assessment.entities.AssessmentQuestionTestcaseType
import io.correcta.assessment.entities.ProgrammingLanguage
import io.correcta.assessment.models.response.GetInternalAssessmentQuestionResponse
import io.correcta.assessment.service.AssessmentQuestionService
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalAssessmentQuestionsControllerImpl(
  val assessmentQuestionService: AssessmentQuestionService,
) : InternalAssessmentQuestionsController {

  override fun getQuestion(
    assessmentId: String,
    questionId: String,
    language: ProgrammingLanguage,
    types: List<AssessmentQuestionTestcaseType>?,
  ): GetInternalAssessmentQuestionResponse = assessmentQuestionService.getInternalQuestion(assessmentId, questionId, language, types)
}