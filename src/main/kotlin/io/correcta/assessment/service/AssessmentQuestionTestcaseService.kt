package io.correcta.assessment.service

import io.correcta.assessment.entities.AssessmentQuestion
import io.correcta.assessment.entities.AssessmentQuestionTestcase
import io.correcta.assessment.entities.AssessmentQuestionTestcaseType
import io.correcta.assessment.exceptions.BadRequestException
import io.correcta.assessment.exceptions.ConflictException
import io.correcta.assessment.exceptions.NotFoundException
import io.correcta.assessment.models.request.BulkUpdateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.request.CreateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.request.ImportAssessmentQuestionTestcasesRequest
import io.correcta.assessment.models.request.UpdateAssessmentQuestionTestcaseRequest
import io.correcta.assessment.models.response.GetAssessmentQuestionTestcaseResponse
import io.correcta.assessment.repositories.AssessmentQuestionRepository
import io.correcta.assessment.repositories.AssessmentQuestionTestcaseRepository
import io.correcta.assessment.repositories.AssessmentSessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AssessmentQuestionTestcaseService(
    val assessmentQuestionTestcaseRepository: AssessmentQuestionTestcaseRepository,
    val assessmentQuestionRepository: AssessmentQuestionRepository,
    val assessmentAccessService: AssessmentAccessService,
    val assessmentSessionRepository: AssessmentSessionRepository,
) {


    @Transactional(readOnly = true)
    fun getTestcases(assessmentId: String, questionId: String): List<GetAssessmentQuestionTestcaseResponse> {
        val assessment =
            assessmentAccessService.findAssessmentWithVisibleQuestions(assessmentId)

        val membership =
            assessmentAccessService.requireCohortMember(assessment.cohortId!!)
        val isFaculty = membership.isFaculty()

        assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.")

        val testcases = assessmentQuestionTestcaseRepository.findByQuestionIdOrderByPositionAsc(questionId)

        return testcases.filter { testcase ->
            val isGenerallyVisible = testcase.type.isListVisible

            isGenerallyVisible || isFaculty
        }.map { testcase ->
            GetAssessmentQuestionTestcaseResponse.fromTestcase(testcase, isFaculty)
        }
    }


    @Transactional(readOnly = true)
    fun getInternalTestcases(
        assessmentId: String,
        questionId: String,
        types: Collection<AssessmentQuestionTestcaseType>?
    ): List<GetAssessmentQuestionTestcaseResponse> {
        assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.")

        val testcases = if (types.isNullOrEmpty()) {
            assessmentQuestionTestcaseRepository.findByQuestionIdOrderByPositionAsc(questionId)
        } else {
            assessmentQuestionTestcaseRepository.findByQuestionIdAndTypeInOrderByPositionAsc(questionId, types)
        }

        return testcases.map { GetAssessmentQuestionTestcaseResponse.fromTestcase(it, isFaculty = true) }
    }


    @Transactional
    fun createTestcase(
        assessmentId: String,
        questionId: String,
        request: CreateAssessmentQuestionTestcaseRequest
    ): GetAssessmentQuestionTestcaseResponse {
        assessmentAccessService.findManageableAssessment(assessmentId)

        val question = assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.")

        val existingTestcase = request.position?.let { position ->
            assessmentQuestionTestcaseRepository.findByQuestionIdAndPosition(questionId, position)
        }

        if (existingTestcase != null) {
            throw ConflictException("Já existe um caso de teste na posição ${request.position}.")
        }

        val position = request.position ?: question.nextAvailablePosition

        val testcase = assessmentQuestionTestcaseRepository.save(
            AssessmentQuestionTestcase(
                question = question,
                position = position,
                input = request.input,
                expectedOutput = request.expectedOutput,
                timeLimitSeconds = request.timeLimitSeconds,
                memoryLimitMB = request.memoryLimitMB,
                type = request.type!!
            )
        )

        return GetAssessmentQuestionTestcaseResponse.fromTestcase(testcase, isFaculty = true)
    }


    @Transactional
    fun updateTestcase(
        assessmentId: String,
        questionId: String,
        testcaseId: String,
        request: UpdateAssessmentQuestionTestcaseRequest
    ): GetAssessmentQuestionTestcaseResponse {
        assessmentAccessService.findManageableAssessment(assessmentId)

        assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.")

        val testcase = assessmentQuestionTestcaseRepository.findByIdAndQuestionId(testcaseId, questionId)
            ?: throw NotFoundException("Caso de teste não encontrado.")

        request.position?.let { position ->
            if (position != testcase.position) {
                val existingTestcase =
                    assessmentQuestionTestcaseRepository.findByQuestionIdAndPosition(questionId, position)
                if (existingTestcase != null) {
                    throw ConflictException("Já existe um caso de teste na posição $position.")
                }
                testcase.position = position
            }
        }

        request.input.let { input ->
            testcase.input = input
        }

        request.expectedOutput?.let { expectedOutput ->
            testcase.expectedOutput = expectedOutput
        }

        request.timeLimitSeconds.let { timeLimitSeconds ->
            testcase.timeLimitSeconds = timeLimitSeconds
        }

        request.memoryLimitMB.let { memoryLimitMB ->
            testcase.memoryLimitMB = memoryLimitMB
        }

        request.type?.let { type ->
            testcase.type = type
        }

        val updatedTestcase = assessmentQuestionTestcaseRepository.save(testcase)

        return GetAssessmentQuestionTestcaseResponse.fromTestcase(updatedTestcase, isFaculty = true)
    }


    @Transactional
    fun bulkUpdateTestcases(
        assessmentId: String,
        questionId: String,
        request: BulkUpdateAssessmentQuestionTestcaseRequest
    ): List<GetAssessmentQuestionTestcaseResponse> {
        assessmentAccessService.findManageableAssessment(assessmentId)

        assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.")

        val testcases = assessmentQuestionTestcaseRepository.findByQuestionIdAndIdInOrderByPositionAsc(
            questionId,
            request.testCaseIds
        )

        if (testcases.size != request.testCaseIds.toSet().size) {
            throw NotFoundException("Caso de teste não encontrado.")
        }

        val updatedTestcases = testcases.map { testcase ->
            request.timeLimitSeconds?.let { timeLimitSeconds ->
                testcase.timeLimitSeconds = timeLimitSeconds
            }

            request.memoryLimitMB?.let { memoryLimitMB ->
                testcase.memoryLimitMB = memoryLimitMB
            }

            request.type?.let { type ->
                testcase.type = type
            }

            assessmentQuestionTestcaseRepository.save(testcase)
        }

        return updatedTestcases.map { testcase ->
            GetAssessmentQuestionTestcaseResponse.fromTestcase(testcase, isFaculty = true)
        }
    }


    @Transactional
    fun deleteTestcase(assessmentId: String, questionId: String, testcaseId: String) {
        assessmentAccessService.findManageableAssessment(assessmentId)

        assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.")

        val testcase = assessmentQuestionTestcaseRepository.findByIdAndQuestionId(testcaseId, questionId)
            ?: throw NotFoundException("Caso de teste não encontrado.")

        requireGradingTestcasesKept(assessmentId, listOf(testcase))
        assessmentQuestionTestcaseRepository.delete(testcase)
    }


    @Transactional
    fun bulkDeleteTestcases(assessmentId: String, questionId: String, testcaseIds: List<String>) {
        assessmentAccessService.findManageableAssessment(assessmentId)

        assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.")

        if (testcaseIds.isEmpty()) {
            throw BadRequestException("Selecione ao menos um caso de teste.")
        }

        val testcases =
            assessmentQuestionTestcaseRepository.findByQuestionIdAndIdInOrderByPositionAsc(questionId, testcaseIds)

        if (testcases.size != testcaseIds.toSet().size) {
            throw NotFoundException("Caso de teste não encontrado.")
        }

        requireGradingTestcasesKept(assessmentId, testcases)
        assessmentQuestionTestcaseRepository.deleteAll(testcases)
    }


    @Transactional
    fun importTestcases(
        assessmentId: String,
        questionId: String,
        request: ImportAssessmentQuestionTestcasesRequest,
        inputs: org.springframework.web.multipart.MultipartFile,
        outputs: org.springframework.web.multipart.MultipartFile
    ): List<GetAssessmentQuestionTestcaseResponse> {
        assessmentAccessService.findManageableAssessment(assessmentId)

        val question = assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.")

        val inputLines = inputs.inputStream.bufferedReader().readLines()
        val outputLines = outputs.inputStream.bufferedReader().readLines()

        if (inputLines.size != outputLines.size) {
            throw BadRequestException("O número de linhas de entrada e saída não corresponde.")
        }

        val nextAvailablePosition = question.nextAvailablePosition

        val testcases = inputLines.zip(outputLines).mapIndexed { index, (input, output) ->
            AssessmentQuestionTestcase(
                question = question,
                position = nextAvailablePosition + index,
                input = input,
                expectedOutput = output,
                timeLimitSeconds = request.timeLimitSeconds,
                memoryLimitMB = request.memoryLimitMB,
                type = request.type!!
            )
        }

        val savedTestcases = assessmentQuestionTestcaseRepository.saveAll(testcases)

        return savedTestcases.map { testcase ->
            GetAssessmentQuestionTestcaseResponse.fromTestcase(testcase, isFaculty = true)
        }
    }


    @Transactional
    fun reorderTestcases(
        assessmentId: String,
        questionId: String,
        newOrder: List<String>
    ): List<GetAssessmentQuestionTestcaseResponse> {
        assessmentAccessService.findManageableAssessment(assessmentId)

        assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.")

        val testcases = assessmentQuestionTestcaseRepository.findByQuestionIdOrderByPositionAsc(questionId)

        if (newOrder.size != testcases.size || !newOrder.containsAll(testcases.map { it.id!! })) {
            throw BadRequestException("A nova ordem de casos de teste não corresponde aos casos de teste existentes.")
        }

        val testcasesById = testcases.associateBy { it.id!! }
        val reorderedTestcases = newOrder.map { testcaseId -> testcasesById.getValue(testcaseId) }

        reorderedTestcases.forEachIndexed { index, testcase -> testcase.position = -(index + 1) }
        assessmentQuestionTestcaseRepository.saveAllAndFlush(reorderedTestcases)

        reorderedTestcases.forEachIndexed { index, testcase -> testcase.position = index + 1 }
        val savedTestcases = assessmentQuestionTestcaseRepository.saveAll(reorderedTestcases)

        return savedTestcases.map { testcase ->
            GetAssessmentQuestionTestcaseResponse.fromTestcase(testcase, isFaculty = true)
        }
    }


    private val AssessmentQuestion.nextAvailablePosition: Int
        get() {
            val maxPosition = assessmentQuestionTestcaseRepository.findMaxPositionByQuestionId(this.id!!)

            return (maxPosition ?: 0) + 1
        }


    private fun requireGradingTestcasesKept(assessmentId: String, testcases: List<AssessmentQuestionTestcase>) {
        if (testcases.any { it.type != AssessmentQuestionTestcaseType.PUBLIC } && assessmentSessionRepository.existsByAssessmentId(
                assessmentId
            )) {
            throw ConflictException("Estudantes já iniciaram a atividade: casos de teste privados e de avaliação não podem ser excluídos.")
        }
    }
}
