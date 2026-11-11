package io.correcta.assessment.service

import io.correcta.assessment.entities.AssessmentSessionState
import io.correcta.assessment.repositories.AssessmentSessionRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduled job that stores the expiry of running sessions whose time ran out, so `session.ended` is sent even without requests.
 */
@Component
class AssessmentSessionSweeper(
  val assessmentSessionRepository: AssessmentSessionRepository,
  val assessmentSessionService: AssessmentSessionService,
) {

  private val log: Logger = LoggerFactory.getLogger(AssessmentSessionSweeper::class.java)

  /** Refreshes every running session. A failure in one session does not stop the others. */
  @Scheduled(
    fixedDelayString = "\${sessions.sweep-interval-ms:60000}",
    initialDelayString = "\${sessions.sweep-interval-ms:60000}",
  )
  fun sweep() {
    assessmentSessionRepository.findByState(AssessmentSessionState.IN_PROGRESS).forEach { session ->
      try {
        assessmentSessionService.refreshSession(session.id!!)
      } catch (exception: RuntimeException) {
        log.error("Failed to refresh session {}", session.id, exception)
      }
    }
  }
}