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
import io.correcta.assessment.repositories.AssessmentSessionRepository
import io.correcta.assessment.utils.SlugExt.toSlug
import io.correcta.assessment.utils.SlugExt.uniqueSlug
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AssessmentQuestionService(
    val assessmentQuestionRepository: AssessmentQuestionRepository,
    val assessmentAccessService: AssessmentAccessService,
    val assessmentSessionRepository: AssessmentSessionRepository,
) {

    

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

    


    @Transactional
    fun createQuestion(assessmentId: String, request: CreateAssessmentQuestionRequest): GetAssessmentQuestionResponse {
        val assessment = assessmentAccessService.findManageableAssessment(assessmentId)
        val position = request.position ?: nextPosition(assessmentId)

        requirePositionAvailable(assessmentId, position, questionId = null)

        val question = assessmentQuestionRepository.save(
            AssessmentQuestion(
                assessment = assessment,
                position = position,
                title = request.title.trim(),
                slug = request.slug?.also { requireSlugAvailable(assessmentId, it, questionId = null) }
                    ?: uniqueSlug(request.title.toSlug(FALLBACK_SLUG)) {
                        it in RESERVED_SLUGS || assessmentQuestionRepository.existsByAssessmentIdAndSlug(assessmentId, it)
                    },
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

        if (request.maximumGrade!!.compareTo(question.maximumGrade) != 0 && assessmentSessionRepository.existsByAssessmentId(assessmentId)) {
            throw ConflictException("Estudantes já iniciaram a atividade: a nota máxima da questão não pode mudar.")
        }

        question.position = position
        question.title = request.title.trim()
        request.slug?.takeIf { it != question.slug }?.let { slug ->
            requireSlugAvailable(assessmentId, slug, questionId)
            question.slug = slug
        }
        question.statement = request.statement.trim()
        question.codeDrafts.clear()
        question.codeDrafts.addAll(codeDraftsOf(request.codeDrafts))
        question.maximumGrade = request.maximumGrade!!
        question.allowCopyPasting = request.allowCopyPasting

        return GetAssessmentQuestionResponse.fromQuestion(assessmentQuestionRepository.saveAndFlush(question))
    }

    


    @Transactional
    fun deleteQuestion(assessmentId: String, questionId: String) {
        val assessment = assessmentAccessService.findManageableAssessment(assessmentId)
        val question = findQuestion(assessmentId, questionId)

        if (assessmentSessionRepository.existsByAssessmentId(assessmentId)) {
            throw ConflictException("Estudantes já iniciaram a atividade: a questão não pode ser excluída.")
        }

        if (assessment.state == AssessmentState.PUBLISHED && assessmentQuestionRepository.countByAssessmentId(assessmentId) <= 1) {
            throw ConflictException("Uma atividade publicada precisa de ao menos uma questão.")
        }

        assessmentQuestionRepository.delete(question)
    }

    

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

    private fun requireSlugAvailable(assessmentId: String, slug: String, questionId: String?) {
        if (slug in RESERVED_SLUGS) throw BadRequestException("O slug '$slug' é reservado.")

        val taken = if (questionId == null) {
            assessmentQuestionRepository.existsByAssessmentIdAndSlug(assessmentId, slug)
        } else {
            assessmentQuestionRepository.existsByAssessmentIdAndSlugAndIdNot(assessmentId, slug, questionId)
        }

        if (taken) throw ConflictException("Já existe uma questão com o slug '$slug' nesta atividade.")
    }

    companion object {
        private const val FALLBACK_SLUG = "questao"

        private val RESERVED_SLUGS = setOf("new")
    }
}
