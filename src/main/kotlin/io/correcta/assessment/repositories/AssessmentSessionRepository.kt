package io.correcta.assessment.repositories

import io.correcta.assessment.entities.AssessmentSession
import io.correcta.assessment.entities.AssessmentSessionState
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Persistence of [AssessmentSession]. The `lockBy` queries take a pessimistic write lock until the transaction ends.
 */
@Repository
interface AssessmentSessionRepository : JpaRepository<AssessmentSession, String> {
  fun findByAssessmentId(assessmentId: String, pageable: Pageable): Page<AssessmentSession>

  fun findByAssessmentIdAndState(
    assessmentId: String,
    state: AssessmentSessionState,
    pageable: Pageable,
  ): Page<AssessmentSession>

  fun findByIdAndAssessmentId(id: String, assessmentId: String): AssessmentSession?

  fun findByAssessmentIdAndStudentProfileId(assessmentId: String, studentProfileId: String): AssessmentSession?

  fun findByAssessmentIdInAndStudentProfileId(
    assessmentIds: Collection<String>,
    studentProfileId: String,
  ): List<AssessmentSession>

  fun findByStudentProfileId(studentProfileId: String, pageable: Pageable): Page<AssessmentSession>

  fun findByStudentProfileIdAndState(
    studentProfileId: String,
    state: AssessmentSessionState,
    pageable: Pageable,
  ): Page<AssessmentSession>

  fun findByAssessmentIdAndState(assessmentId: String, state: AssessmentSessionState): List<AssessmentSession>

  fun findByState(state: AssessmentSessionState): List<AssessmentSession>

  fun existsByAssessmentId(assessmentId: String): Boolean

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from AssessmentSession s where s.assessment.id = :assessmentId and s.studentProfileId = :studentProfileId")
  fun lockByAssessmentIdAndStudentProfileId(assessmentId: String, studentProfileId: String): AssessmentSession?

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from AssessmentSession s where s.id = :id and s.assessment.id = :assessmentId")
  fun lockByIdAndAssessmentId(id: String, assessmentId: String): AssessmentSession?

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from AssessmentSession s where s.id = :id")
  fun lockById(id: String): AssessmentSession?

  @Query("select s.state as state, count(s) as total from AssessmentSession s where s.assessment.id = :assessmentId group by s.state")
  fun countByStateOfAssessment(assessmentId: String): List<StateCount>

  /** Projection of [countByStateOfAssessment]. */
  interface StateCount {
    val state: AssessmentSessionState
    val total: Long
  }
}