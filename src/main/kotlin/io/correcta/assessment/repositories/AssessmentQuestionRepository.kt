package io.correcta.assessment.repositories

import io.correcta.assessment.entities.AssessmentQuestion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
interface AssessmentQuestionRepository : JpaRepository<AssessmentQuestion, String> {
    fun findByAssessmentIdOrderByPositionAsc(assessmentId: String): List<AssessmentQuestion>

    fun findByIdAndAssessmentId(id: String, assessmentId: String): AssessmentQuestion?

    fun findByAssessmentIdAndPosition(assessmentId: String, position: Int): AssessmentQuestion?

    fun findTopByAssessmentIdOrderByPositionDesc(assessmentId: String): AssessmentQuestion?

    fun existsByAssessmentId(assessmentId: String): Boolean

    fun countByAssessmentId(assessmentId: String): Long

    fun deleteByAssessmentId(assessmentId: String)

    @Query(
        """
        select q.assessment.id as assessmentId, count(q) as questionCount, coalesce(sum(q.maximumGrade), 0) as totalGrade
        from AssessmentQuestion q
        where q.assessment.id in :assessmentIds
        group by q.assessment.id
        """
    )
    fun summarizeByAssessmentIds(assessmentIds: Collection<String>): List<QuestionSummary>

    /** how many questions an assessment has and what they are worth together. */
    interface QuestionSummary {
        val assessmentId: String
        val questionCount: Long
        val totalGrade: BigDecimal
    }
}
