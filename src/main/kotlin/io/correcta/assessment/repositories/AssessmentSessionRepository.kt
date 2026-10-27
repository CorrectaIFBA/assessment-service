package io.correcta.assessment.repositories

import io.correcta.assessment.entities.AssessmentSession
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AssessmentSessionRepository : JpaRepository<AssessmentSession, String> {
    fun findByAssessmentId(assessmentId: String, pageable: Pageable): Page<AssessmentSession>

    fun findByIdAndAssessmentId(id: String, assessmentId: String): AssessmentSession?

    fun findByAssessmentIdAndStudentProfileId(assessmentId: String, studentProfileId: String): AssessmentSession?

    fun deleteByAssessmentId(assessmentId: String)
}
