package io.correcta.assessment.service

import io.correcta.assessment.entities.AssessmentQuestion
import io.correcta.assessment.entities.AssessmentQuestionTestcase
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AssessmentQuestionTestcaseService(
    val assessmentQuestionTestcaseRepository: AssessmentQuestionTestcaseRepository,
    val assessmentQuestionRepository: AssessmentQuestionRepository,
    val assessmentAccessService: AssessmentAccessService,
) {

    /**
     * lists the test cases of a question ordered by position, following the same visibility rules as the questions.
     * students only get the test cases whose type is list visible, with hidden values left null.
     */
    @Transactional(readOnly = true)
    fun getTestcases(assessmentId: String, questionId: String): List<GetAssessmentQuestionTestcaseResponse> {
        val assessment =
            assessmentAccessService.findAssessmentWithVisibleQuestions(assessmentId) // ensure read access to the assessment and question

        val membership =
            assessmentAccessService.requireCohortMember(assessment.cohortId!!) // ensure the user is a member of the cohort
        val isFaculty = membership.isFaculty()

        assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.") // ensure the question belongs to the assessment

        val testcases = assessmentQuestionTestcaseRepository.findByQuestionIdOrderByPositionAsc(questionId)

        return testcases.filter { testcase ->
            val isGenerallyVisible = testcase.type.isListVisible

            isGenerallyVisible || isFaculty
        }.map { testcase ->
            GetAssessmentQuestionTestcaseResponse.fromTestcase(testcase, isFaculty)
        }
    }

    /**
     * allows a teacher to add a test case to a question of an assessment in a cohort they teach.
     * appended after the last test case when no position is given; a taken position is a conflict.
     */
    @Transactional
    fun createTestcase(
        assessmentId: String,
        questionId: String,
        request: CreateAssessmentQuestionTestcaseRequest
    ): GetAssessmentQuestionTestcaseResponse {
        assessmentAccessService.findManageableAssessment(assessmentId) // ensure write access to the assessment and question

        val question = assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.") // ensure the question belongs to the assessment

        val existingTestcase = request.position?.let { position ->
            assessmentQuestionTestcaseRepository.findByQuestionIdAndPosition(questionId, position)
        }

        if (existingTestcase != null) {
            throw ConflictException("Já existe um caso de teste na posição ${request.position}.") // should never happen, because frontend should prevent it, but just in case
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

    /**
     * allows a teacher to edit a test case of a question of an assessment in a cohort they teach.
     */
    @Transactional
    fun updateTestcase(
        assessmentId: String,
        questionId: String,
        testcaseId: String,
        request: UpdateAssessmentQuestionTestcaseRequest
    ): GetAssessmentQuestionTestcaseResponse {
        assessmentAccessService.findManageableAssessment(assessmentId) // ensure write access to the assessment and question

        assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.") // ensure the question belongs to the assessment

        val testcase = assessmentQuestionTestcaseRepository.findByIdAndQuestionId(testcaseId, questionId)
            ?: throw NotFoundException("Caso de teste não encontrado.") // ensure the testcase belongs to the question

        request.position?.let { position ->
            if (position != testcase.position) {
                val existingTestcase = assessmentQuestionTestcaseRepository.findByQuestionIdAndPosition(questionId, position)
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

    /**
     * allows a teacher to update multiple test cases of a question of an assessment in a cohort they teach.
     */
    @Transactional
    fun bulkUpdateTestcases(
        assessmentId: String,
        questionId: String,
        request: BulkUpdateAssessmentQuestionTestcaseRequest
    ): List<GetAssessmentQuestionTestcaseResponse> {
        assessmentAccessService.findManageableAssessment(assessmentId) // ensure write access to the assessment and question

        assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.") // ensure the question belongs to the assessment

        val testcases = assessmentQuestionTestcaseRepository.findByQuestionIdAndIdInOrderByPositionAsc(questionId, request.testCaseIds)

        if (testcases.size != request.testCaseIds.toSet().size) {
            throw NotFoundException("Caso de teste não encontrado.") // every id must belong to the question
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

    /**
     * allows a teacher to remove a test case of a question of an assessment in a cohort they teach.
     */
    @Transactional
    fun deleteTestcase(assessmentId: String, questionId: String, testcaseId: String) {
        assessmentAccessService.findManageableAssessment(assessmentId) // ensure write access to the assessment and question

        assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.") // ensure the question belongs to the assessment

        val testcase = assessmentQuestionTestcaseRepository.findByIdAndQuestionId(testcaseId, questionId)
            ?: throw NotFoundException("Caso de teste não encontrado.") // ensure the testcase belongs to the question

        assessmentQuestionTestcaseRepository.delete(testcase)
    }

    /**
     * allows a teacher to remove multiple test cases of a question of an assessment in a cohort they teach.
     * the remaining test cases keep their positions; the order is what counts, not the number.
     */
    @Transactional
    fun bulkDeleteTestcases(assessmentId: String, questionId: String, testcaseIds: List<String>) {
        assessmentAccessService.findManageableAssessment(assessmentId) // ensure write access to the assessment and question

        assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.") // ensure the question belongs to the assessment

        if (testcaseIds.isEmpty()) {
            throw BadRequestException("Selecione ao menos um caso de teste.")
        }

        val testcases = assessmentQuestionTestcaseRepository.findByQuestionIdAndIdInOrderByPositionAsc(questionId, testcaseIds)

        if (testcases.size != testcaseIds.toSet().size) {
            throw NotFoundException("Caso de teste não encontrado.") // every id must belong to the question
        }

        assessmentQuestionTestcaseRepository.deleteAll(testcases)
    }

    /**
     * allows a teacher to import test cases from inputs and outputs file to a question of an assessment in a cohort they teach.
     */
    @Transactional
    fun importTestcases(
        assessmentId: String,
        questionId: String,
        request: ImportAssessmentQuestionTestcasesRequest,
        inputs: org.springframework.web.multipart.MultipartFile,
        outputs: org.springframework.web.multipart.MultipartFile
    ): List<GetAssessmentQuestionTestcaseResponse> {
        assessmentAccessService.findManageableAssessment(assessmentId) // ensure write access to the assessment and question

        val question = assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.") // ensure the question belongs to the assessment

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

    /**
     * allows a teacher to reorder a batch of test cases of a question of an assessment in a cohort they teach.
     */
    @Transactional
    fun reorderTestcases(
        assessmentId: String,
        questionId: String,
        newOrder: List<String>
    ): List<GetAssessmentQuestionTestcaseResponse> {
        assessmentAccessService.findManageableAssessment(assessmentId) // ensure write access to the assessment and question

        assessmentQuestionRepository.findByIdAndAssessmentId(questionId, assessmentId)
            ?: throw NotFoundException("Questão não encontrada.") // ensure the question belongs to the assessment

        val testcases = assessmentQuestionTestcaseRepository.findByQuestionIdOrderByPositionAsc(questionId)

        if (newOrder.size != testcases.size || !newOrder.containsAll(testcases.map { it.id!! })) {
            throw BadRequestException("A nova ordem de casos de teste não corresponde aos casos de teste existentes.")
        }

        val testcasesById = testcases.associateBy { it.id!! }
        val reorderedTestcases = newOrder.map { testcaseId -> testcasesById.getValue(testcaseId) }

        // positions are unique per question and the constraint is checked row by row,
        // so every test case first moves to a free negative slot, then to its final place
        reorderedTestcases.forEachIndexed { index, testcase -> testcase.position = -(index + 1) }
        assessmentQuestionTestcaseRepository.saveAllAndFlush(reorderedTestcases)

        reorderedTestcases.forEachIndexed { index, testcase -> testcase.position = index + 1 }
        val savedTestcases = assessmentQuestionTestcaseRepository.saveAll(reorderedTestcases)

        return savedTestcases.map { testcase ->
            GetAssessmentQuestionTestcaseResponse.fromTestcase(testcase, isFaculty = true)
        }
    }

    /**
     * Returns the next available position for a test case in a question.
     */
    private val AssessmentQuestion.nextAvailablePosition: Int
        get() {
            val maxPosition = assessmentQuestionTestcaseRepository.findMaxPositionByQuestionId(this.id!!)

            return (maxPosition ?: 0) + 1
        }
}
