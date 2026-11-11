package io.correcta.assessment.repositories

import io.correcta.assessment.entities.AssessmentQuestion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal

/** Persistence of [AssessmentQuestion]. */
@Repository
interface AssessmentQuestionRepository : JpaRepository<AssessmentQuestion, String> {
  fun findByAssessmentIdOrderByPositionAsc(assessmentId: String): List<AssessmentQuestion>

  fun findByIdAndAssessmentId(id: String, assessmentId: String): AssessmentQuestion?

  fun findByAssessmentIdAndPosition(assessmentId: String, position: Int): AssessmentQuestion?

  fun findTopByAssessmentIdOrderByPositionDesc(assessmentId: String): AssessmentQuestion?

  fun existsByAssessmentId(assessmentId: String): Boolean

  fun existsByAssessmentIdAndSlug(assessmentId: String, slug: String): Boolean

  fun existsByAssessmentIdAndSlugAndIdNot(assessmentId: String, slug: String, id: String): Boolean

  fun countByAssessmentId(assessmentId: String): Long

  /**
   * Counts the questions and sums their maximum grades per assessment. Assessments without questions are absent.
   */
  @Query(
    """
        select q.assessment.id as assessmentId, count(q) as questionCount, coalesce(sum(q.maximumGrade), 0) as totalGrade
        from AssessmentQuestion q
        where q.assessment.id in :assessmentIds
        group by q.assessment.id
        """,
  )
  fun summarizeByAssessmentIds(assessmentIds: Collection<String>): List<QuestionSummary>

  /** Projection of [summarizeByAssessmentIds]. */
  interface QuestionSummary {
    val assessmentId: String
    val questionCount: Long
    val totalGrade: BigDecimal
  }
}