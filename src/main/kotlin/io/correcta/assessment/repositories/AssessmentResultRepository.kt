package io.correcta.assessment.repositories

import io.correcta.assessment.entities.AssessmentResult
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AssessmentResultRepository : JpaRepository<AssessmentResult, String> {
  fun findByAssessmentIdInAndStudentProfileId(
    assessmentIds: Collection<String>,
    studentProfileId: String,
  ): List<AssessmentResult>
}