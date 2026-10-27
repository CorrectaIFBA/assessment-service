package io.correcta.assessment.service

import io.correcta.assessment.entities.AssessmentQuestion
import io.correcta.assessment.entities.AssessmentState
import io.correcta.assessment.entities.CodeDraft
import io.correcta.assessment.exceptions.BadRequestException
import io.correcta.assessment.exceptions.ConflictException
import io.correcta.assessment.exceptions.NotFoundException
import io.correcta.assessment.models.request.CodeDraftRequest
import io.correcta.assessment.models.request.CreateAssessmentQuestionRequest
import io.correcta.assessment.models.request.UpdateAssessmentQuestionRequest
import io.correcta.assessment.models.response.GetAssessmentQuestionResponse
import io.correcta.assessment.repositories.AssessmentQuestionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AssessmentQuestionService(
    val assessmentQuestionRepository: AssessmentQuestionRepository,
    val assessmentAccessService: AssessmentAccessService,
) {

    /**
     * lists the questions of an assessment ordered by position. students only see them once they started the assessment.
     */
    @Transactional(readOnly = true)
    fun getQuestions(assessmentId: String): List<GetAssessmentQuestionResponse> {
        assessmentAccessService.findAssessmentWithVisibleQuestions(assessmentId)

        return assessmentQuestionRepository
            .findByAssessmentIdOrderByPositionAsc(assessmentId)
            .map(GetAssessmentQuestionResponse::fromQuestion)
    }

    @Transactional(readOnly = true)
    fun getQuestion(assessmentId: String, questionId: String): GetAssessmentQuestionResponse {
        assessmentAccessService.findAssessmentWithVisibleQuestions(assessmentId)

        return GetAssessmentQuestionResponse.fromQuestion(findQuestion(assessmentId, questionId))
    }

    /**
     * the question is appended after the last one when no position is given.
     * the assessment row is locked, so concurrent creations never compute the same position.
     */
    @Transactional
    fun createQuestion(assessmentId: String, request: CreateAssessmentQuestionRequest): GetAssessmentQuestionResponse {
        val assessment = assessmentAccessService.findManageableAssessment(assessmentId)
        val position = request.position ?: nextPosition(assessmentId)

        requirePositionAvailable(assessmentId, position, questionId = null)

        // non-null fields are guaranteed by @NotNull; nullable only so a missing field fails validation instead of deserialization
        val question = assessmentQuestionRepository.save(
            AssessmentQuestion(
                assessment = assessment,
                position = position,
                statement = request.statement.trim(),
                codeDrafts = codeDraftsOf(request.codeDrafts),
                maximumGrade = request.maximumGrade!!,
                allowCopyPasting = request.allowCopyPasting,
            )
        )

        return GetAssessmentQuestionResponse.fromQuestion(question)
    }

    @Transactional
    fun updateQuestion(
        assessmentId: String,
        questionId: String,
        request: UpdateAssessmentQuestionRequest
    ): GetAssessmentQuestionResponse {
        assessmentAccessService.findManageableAssessment(assessmentId)
        val question = findQuestion(assessmentId, questionId)
        val position = request.position!!

        requirePositionAvailable(assessmentId, position, questionId)

        question.position = position
        question.statement = request.statement.trim()
        // replaced in place: Hibernate tracks the collection instance it loaded
        question.codeDrafts.clear()
        question.codeDrafts.addAll(codeDraftsOf(request.codeDrafts))
        question.maximumGrade = request.maximumGrade!!
        question.allowCopyPasting = request.allowCopyPasting

        return GetAssessmentQuestionResponse.fromQuestion(assessmentQuestionRepository.saveAndFlush(question))
    }

    /**
     * a published assessment must keep at least one question, mirroring the publish rule.
     */
    @Transactional
    fun deleteQuestion(assessmentId: String, questionId: String) {
        val assessment = assessmentAccessService.findManageableAssessment(assessmentId)
        val question = findQuestion(assessmentId, questionId)

        if (assessment.state == AssessmentState.PUBLISHED && assessmentQuestionRepository.countByAssessmentId(assessmentId) <= 1) {
            throw ConflictException("Uma atividade publicada precisa de ao menos uma questão.")
        }

        assessmentQuestionRepository.delete(question)
    }

    /**
     * each language may be allowed once; a second draft for it would leave the student two starting points.
     */
    private fun codeDraftsOf(requests: List<CodeDraftRequest>): MutableList<CodeDraft> {
        val drafts = requests.map(CodeDraftRequest::toCodeDraft)

        if (drafts.map { it.language }.toSet().size != drafts.size) {
            throw BadRequestException("Cada linguagem pode ter apenas um código inicial.")
        }

        return drafts.toMutableList()
    }

    private fun nextPosition(assessmentId: String): Int {
        return assessmentQuestionRepository.findTopByAssessmentIdOrderByPositionDesc(assessmentId)
            ?.position?.plus(1)
            ?: 0
    }

    private fun requirePositionAvailable(assessmentId: String, position: Int, questionId: String?) {
        val occupant = assessmentQuestionRepository.findByAssessmentIdAndPosition(assessmentId, position)

        if (occupant != null && occupant.id != questionId) {
            throw ConflictException("Já existe uma questão na posição $position.")
        }
    }

    private fun findQuestion(assessmentId: String, questionId: String): AssessmentQuestion {
        return assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.")
    }
}
