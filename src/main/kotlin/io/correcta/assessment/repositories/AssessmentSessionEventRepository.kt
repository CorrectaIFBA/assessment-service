package io.correcta.assessment.repositories

import io.correcta.assessment.entities.AssessmentSessionEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AssessmentSessionEventRepository : JpaRepository<AssessmentSessionEvent, String> {
  fun findBySessionIdOrderByOccurredAtAsc(sessionId: String): List<AssessmentSessionEvent>
}