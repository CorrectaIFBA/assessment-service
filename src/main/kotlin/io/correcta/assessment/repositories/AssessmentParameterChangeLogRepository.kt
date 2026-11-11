package io.correcta.assessment.repositories

import io.correcta.assessment.entities.AssessmentParameterChangeLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AssessmentParameterChangeLogRepository : JpaRepository<AssessmentParameterChangeLog, String> {
  fun findByAssessmentId(assessmentId: String, pageable: Pageable): Page<AssessmentParameterChangeLog>

  fun deleteByAssessmentId(assessmentId: String)
}