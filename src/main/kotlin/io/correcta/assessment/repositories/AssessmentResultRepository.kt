package io.correcta.assessment.repositories

import io.correcta.assessment.entities.AssessmentResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AssessmentResultRepository : JpaRepository<AssessmentResult, String> {
    fun findByAssessmentId(assessmentId: String, pageable: Pageable): Page<AssessmentResult>

    fun findByIdAndAssessmentId(id: String, assessmentId: String): AssessmentResult?

    fun findByAssessmentIdAndStudentProfileId(assessmentId: String, studentProfileId: String): AssessmentResult?

    fun findByAssessmentIdInAndStudentProfileId(assessmentIds: Collection<String>, studentProfileId: String): List<AssessmentResult>

    fun deleteByAssessmentId(assessmentId: String)
}
