package io.correcta.assessment.repositories

import io.correcta.assessment.entities.AssessmentQuestionTestcase
import io.correcta.assessment.entities.AssessmentQuestionTestcaseType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AssessmentQuestionTestcaseRepository : JpaRepository<AssessmentQuestionTestcase, String> {
  fun findByQuestionIdAndIdInOrderByPositionAsc(
    questionId: String,
    ids: Collection<String>,
  ): List<AssessmentQuestionTestcase>

  fun findByQuestionIdOrderByPositionAsc(questionId: String): List<AssessmentQuestionTestcase>

  fun findByQuestionIdAndTypeInOrderByPositionAsc(
    questionId: String,
    types: Collection<AssessmentQuestionTestcaseType>,
  ): List<AssessmentQuestionTestcase>

  fun findByIdAndQuestionId(id: String, questionId: String): AssessmentQuestionTestcase?

  fun findByQuestionIdAndPosition(questionId: String, position: Int): AssessmentQuestionTestcase?

  @Query("select max(t.position) from AssessmentQuestionTestcase t where t.question.id = :questionId")
  fun findMaxPositionByQuestionId(questionId: String): Int?

  fun findTopByQuestionIdOrderByPositionDesc(questionId: String): AssessmentQuestionTestcase?

  fun countByQuestionId(questionId: String): Long

  fun deleteByQuestionId(questionId: String)
}