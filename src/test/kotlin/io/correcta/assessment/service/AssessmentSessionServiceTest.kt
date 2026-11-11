package io.correcta.assessment.service

import io.correcta.assessment.entities.AssessmentSession
import io.correcta.assessment.entities.AssessmentSessionEvent
import io.correcta.assessment.entities.AssessmentSessionEventType
import io.correcta.assessment.entities.AssessmentSessionState.EXPIRED
import io.correcta.assessment.entities.AssessmentSessionState.IN_PROGRESS
import io.correcta.assessment.entities.AssessmentSessionState.SUBMITTED
import io.correcta.assessment.entities.AssessmentState
import io.correcta.assessment.exceptions.ConflictException
import io.correcta.assessment.exceptions.ForbiddenException
import io.correcta.assessment.exceptions.NotFoundException
import io.correcta.assessment.messaging.Outbox
import io.correcta.assessment.messaging.session.SessionEndedMessage
import io.correcta.assessment.messaging.session.SessionMessaging
import io.correcta.assessment.models.request.PaginationRequest
import io.correcta.assessment.models.request.SessionFilterRequest
import io.correcta.assessment.models.request.SessionTimeChangeRequest
import io.correcta.assessment.repositories.AssessmentParametersRepository
import io.correcta.assessment.repositories.AssessmentSessionEventRepository
import io.correcta.assessment.repositories.AssessmentSessionRepository
import io.correcta.assessment.support.Fixtures.NOW
import io.correcta.assessment.support.Fixtures.assessment
import io.correcta.assessment.support.Fixtures.membership
import io.correcta.assessment.support.Fixtures.parameters
import io.correcta.assessment.support.Fixtures.principal
import io.correcta.assessment.support.Fixtures.session
import io.correcta.assessment.support.anyValue
import io.correcta.assessment.support.mock
import io.correcta.assessment.support.thenReturnFirstArgument
import io.correcta.assessment.support.whenever
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.Clock
import java.time.Duration
import java.time.Instant

class AssessmentSessionServiceTest {

  private val sessionRepository = mock<AssessmentSessionRepository>()
  private val eventRepository = mock<AssessmentSessionEventRepository>()
  private val parametersRepository = mock<AssessmentParametersRepository>()
  private val accessService = mock<AssessmentAccessService>()
  private val entityManager = mock<EntityManager>()
  private val clock = mock<Clock>()
  private val outbox = mock<Outbox>()

  private val service = AssessmentSessionService(sessionRepository, eventRepository, parametersRepository, accessService, entityManager, clock, outbox)

  private var now: Instant = NOW
  private val assessment = assessment(state = AssessmentState.PUBLISHED)
  private val assessmentId = assessment.id!!
  private val events = mutableListOf<AssessmentSessionEvent>()
  private val ended = mutableListOf<SessionEndedMessage>()

  @BeforeEach
  fun setUp() {
    whenever(clock.instant()).thenAnswer { now }
    signIn("student-a", "STUDENT")
    whenever(accessService.findManageableAssessment(assessmentId)).thenReturn(assessment)
    whenever(eventRepository.save(anyValue<AssessmentSessionEvent>())).thenReturnFirstArgument { events += it }
    whenever(sessionRepository.saveAndFlush(anyValue<AssessmentSession>())).thenReturnFirstArgument { it.id = it.id ?: "session-new" }
    doAnswer {
      assertEquals(SessionMessaging.EXCHANGE, it.arguments[0])
      assertEquals(SessionMessaging.SESSION_ENDED_KEY, it.arguments[1])
      ended += it.arguments[2] as SessionEndedMessage
      null
    }.`when`(outbox).enqueue(anyValue(), anyValue(), anyValue())
  }

  private fun signIn(profileId: String, role: String, status: String = "ACTIVE") {
    whenever(accessService.getAuthenticatedProfile()).thenReturn(principal(profileId))
    whenever(accessService.findReadableAssessmentWithMembership(assessmentId))
      .thenReturn(assessment to membership(role = role, status = status, profileId = profileId))
  }

  private fun ownSession(session: AssessmentSession?) {
    whenever(sessionRepository.lockByAssessmentIdAndStudentProfileId(assessmentId, "student-a")).thenReturn(session)
  }

  private fun facultySession(session: AssessmentSession) {
    signIn("faculty-a", "FACULTY")
    whenever(sessionRepository.lockByIdAndAssessmentId(session.id!!, assessmentId)).thenReturn(session)
    whenever(sessionRepository.findByIdAndAssessmentId(session.id!!, assessmentId)).thenReturn(session)
  }

  private fun eventTypes() = events.map { it.type }

  @Test
  fun `exposes its dependencies`() {
    assertSame(sessionRepository, service.assessmentSessionRepository)
    assertSame(eventRepository, service.assessmentSessionEventRepository)
    assertSame(parametersRepository, service.assessmentParametersRepository)
    assertSame(accessService, service.assessmentAccessService)
    assertSame(entityManager, service.entityManager)
    assertSame(clock, service.clock)
    assertSame(outbox, service.outbox)
  }

  @Test
  fun `reading the own session stores an expiry that is due`() {
    assertThrows<NotFoundException> { service.getMySession(assessmentId) }

    ownSession(session(assessment, expiresAt = NOW.plusSeconds(60)))
    assertEquals(IN_PROGRESS, service.getMySession(assessmentId).state)

    now = NOW.plusSeconds(61)
    val expired = service.getMySession(assessmentId)

    assertEquals(EXPIRED, expired.state)
    assertEquals(NOW.plusSeconds(60).toString(), expired.finishedAt)
    assertEquals(listOf(AssessmentSessionEventType.EXPIRED), eventTypes())
    assertEquals("EXPIRED", ended.single().reason)
    assertEquals("session-1", ended.single().sessionId)
  }

  @Test
  fun `faculty, inactive cohorts and unpublished assessments do not start`() {
    signIn("faculty-a", "FACULTY")
    assertThrows<ForbiddenException> { service.startSession(assessmentId) }

    signIn("student-a", "STUDENT", status = "ARCHIVED")
    assertThrows<ConflictException> { service.startSession(assessmentId) }

    signIn("student-a", "STUDENT")
    assessment.state = AssessmentState.ARCHIVED
    assertThrows<ConflictException> { service.startSession(assessmentId) }
    verify(entityManager).refresh(assessment, LockModeType.PESSIMISTIC_WRITE)
  }

  @Test
  fun `starts without limits when the assessment has no parameters`() {
    val (session, created) = service.startSession(assessmentId)

    assertTrue(created)
    assertEquals("session-new", session.id)
    assertNull(session.expiresAt)
    assertTrue(session.rules.canViewDefinitiveTestcaseScoringAfterSubmission)
    assertEquals(listOf(AssessmentSessionEventType.STARTED), eventTypes())
  }

  @Test
  fun `the session expires at the earliest of the time limit and the closing due date`() {
    assessment.dueDate = NOW.plus(Duration.ofMinutes(20))
    whenever(parametersRepository.findByAssessmentId(assessmentId)).thenReturn(parameters(assessment, maximumTimeAllowed = 30, canViewScoring = false))

    val (session, _) = service.startSession(assessmentId)

    assertEquals(NOW.plus(Duration.ofMinutes(20)).toString(), session.expiresAt)
    assertFalse(session.rules.canViewDefinitiveTestcaseScoringAfterSubmission)
  }

  @Test
  fun `late submissions ignore the due date`() {
    assessment.dueDate = NOW.minusSeconds(1)
    whenever(parametersRepository.findByAssessmentId(assessmentId)).thenReturn(parameters(assessment, canBeSubmittedAfterDueDate = true, maximumTimeAllowed = 30))

    val (session, _) = service.startSession(assessmentId)

    assertEquals(NOW.plus(Duration.ofMinutes(30)).toString(), session.expiresAt)
    assertTrue(session.late)
  }

  @Test
  fun `no session starts after the closing due date`() {
    assessment.dueDate = NOW
    whenever(parametersRepository.findByAssessmentId(assessmentId)).thenReturn(parameters(assessment))

    assertThrows<ConflictException> { service.startSession(assessmentId) }
  }

  @Test
  fun `starting again resumes a running session and rejects a finished one`() {
    val running = session(assessment, lastHeartbeatAt = NOW)
    ownSession(running)
    now = NOW.plusSeconds(20)

    val (resumed, created) = service.startSession(assessmentId)

    assertFalse(created)
    assertEquals(20, resumed.openSeconds)
    assertEquals(listOf(AssessmentSessionEventType.RESUMED), eventTypes())

    running.state = SUBMITTED
    assertThrows<ConflictException> { service.startSession(assessmentId) }
  }

  @Test
  fun `heartbeats count short gaps only`() {
    assertThrows<NotFoundException> { service.heartbeat(assessmentId) }

    val running = session(assessment, lastHeartbeatAt = null)
    ownSession(running)

    now = NOW.plusSeconds(30)
    assertEquals(30, service.heartbeat(assessmentId).openSeconds)

    now = NOW.plusSeconds(300)
    assertEquals(30, service.heartbeat(assessmentId).openSeconds)

    now = NOW.plusSeconds(200)
    assertEquals(30, service.heartbeat(assessmentId).openSeconds)
    assertEquals(NOW.plusSeconds(200), running.lastHeartbeatAt)

    running.state = SUBMITTED
    now = NOW.plusSeconds(210)
    assertEquals(SUBMITTED, service.heartbeat(assessmentId).state)
    assertEquals(NOW.plusSeconds(200), running.lastHeartbeatAt)
  }

  @Test
  fun `deliveries submit a running session once without announcing it`() {
    assertThrows<NotFoundException> { service.deliverSession(assessmentId, "student-a", NOW) }

    val running = session(assessment)
    ownSession(running)

    service.deliverSession(assessmentId, "student-a", NOW.minusSeconds(5))
    service.deliverSession(assessmentId, "student-a", NOW.plusSeconds(5))

    assertEquals(SUBMITTED, running.state)
    assertEquals(NOW.minusSeconds(5), running.finishedAt)
    assertEquals(listOf(AssessmentSessionEventType.SUBMITTED), eventTypes())
    verifyNoInteractions(outbox)
  }

  @Test
  fun `lists the own sessions, filtered by state`() {
    val sessions = PageImpl(listOf(session(assessment, expiresAt = NOW.minusSeconds(1))))
    whenever(sessionRepository.findByStudentProfileId(anyValue(), anyValue<Pageable>())).thenReturn(sessions)
    whenever(sessionRepository.findByStudentProfileIdAndState(anyValue(), anyValue(), anyValue<Pageable>())).thenReturn(PageImpl(emptyList()))

    assertEquals(EXPIRED, service.getMySessions(SessionFilterRequest(), PaginationRequest(sortBy = "startedAt")).content.single().state)
    assertTrue(service.getMySessions(SessionFilterRequest(SUBMITTED), PaginationRequest()).content.isEmpty())
    verifyNoInteractions(eventRepository)
  }

  @Test
  fun `a state filter without a page falls back to the unfiltered list`() {
    whenever(sessionRepository.findByStudentProfileId(anyValue(), anyValue<Pageable>())).thenReturn(PageImpl(listOf(session(assessment))))
    assertEquals(1, service.getMySessions(SessionFilterRequest(SUBMITTED), PaginationRequest()).content.size)

    signIn("faculty-a", "FACULTY")
    whenever(sessionRepository.findByAssessmentId(anyValue(), anyValue<Pageable>())).thenReturn(PageImpl(listOf(session(assessment))))
    assertEquals(1, service.getSessions(assessmentId, SessionFilterRequest(SUBMITTED), PaginationRequest()).content.size)
  }

  @Test
  fun `summarizes the own sessions by assessment`() {
    assertTrue(service.summarizeMySessions(emptyList()).isEmpty())

    whenever(sessionRepository.findByAssessmentIdInAndStudentProfileId(listOf(assessmentId), "student-a"))
      .thenReturn(listOf(session(assessment)))

    assertEquals("session-1", service.summarizeMySessions(listOf(assessmentId)).getValue(assessmentId).id)
  }

  @Test
  fun `only faculty follow the sessions of an assessment`() {
    assertThrows<ForbiddenException> { service.getSessions(assessmentId, SessionFilterRequest(), PaginationRequest()) }
    assertThrows<ForbiddenException> { service.countSessions(assessmentId) }
    assertThrows<ForbiddenException> { service.getSession(assessmentId, "session-1") }
    assertThrows<ForbiddenException> { service.getSessionEvents(assessmentId, "session-1") }
  }

  @Test
  fun `faculty list the sessions, filtered by state`() {
    signIn("faculty-a", "FACULTY")
    whenever(parametersRepository.findByAssessmentId(assessmentId)).thenReturn(parameters(assessment, canViewScoring = false))
    whenever(sessionRepository.findByAssessmentId(anyValue(), anyValue<Pageable>())).thenReturn(PageImpl(listOf(session(assessment))))
    whenever(sessionRepository.findByAssessmentIdAndState(anyValue(), anyValue(), anyValue<Pageable>())).thenReturn(PageImpl(emptyList()))

    val all = service.getSessions(assessmentId, SessionFilterRequest(), PaginationRequest())
    assertFalse(all.content.single().rules.canViewDefinitiveTestcaseScoringAfterSubmission)
    assertTrue(service.getSessions(assessmentId, SessionFilterRequest(IN_PROGRESS), PaginationRequest()).content.isEmpty())
  }

  @Test
  fun `counts sessions per state, moving due expiries out of progress`() {
    signIn("faculty-a", "FACULTY")
    whenever(sessionRepository.countByStateOfAssessment(assessmentId)).thenReturn(
      listOf(
        object : AssessmentSessionRepository.StateCount {
          override val state = IN_PROGRESS
          override val total = 2L
        },
      ),
    )
    whenever(sessionRepository.findByAssessmentIdAndState(assessmentId, IN_PROGRESS))
      .thenReturn(listOf(session(assessment, expiresAt = NOW.minusSeconds(1)), session(assessment, id = "session-2")))

    assertEquals(mapOf(IN_PROGRESS to 1L, SUBMITTED to 0L, EXPIRED to 1L), service.countSessions(assessmentId))
  }

  @Test
  fun `faculty read one session and its events`() {
    val session = session(assessment)
    facultySession(session)
    whenever(eventRepository.findBySessionIdOrderByOccurredAtAsc("session-1"))
      .thenReturn(listOf(AssessmentSessionEvent(id = "event-1", session = session, type = AssessmentSessionEventType.STARTED, occurredAt = NOW, receivedAt = NOW)))

    assertEquals("session-1", service.getSession(assessmentId, "session-1").id)
    assertEquals(listOf("event-1"), service.getSessionEvents(assessmentId, "session-1").map { it.id })
    assertThrows<NotFoundException> { service.getSession(assessmentId, "missing") }
  }

  @Test
  fun `reopening needs a published assessment and a finished session`() {
    val session = session(assessment)
    facultySession(session)

    assertThrows<NotFoundException> { service.reopenSession(assessmentId, "missing", SessionTimeChangeRequest(10)) }
    assertThrows<ConflictException> { service.reopenSession(assessmentId, "session-1", SessionTimeChangeRequest(10)) }

    assessment.state = AssessmentState.ARCHIVED
    assertThrows<ConflictException> { service.reopenSession(assessmentId, "session-1", SessionTimeChangeRequest(10)) }
  }

  @Test
  fun `reopening runs the session again for the extra minutes`() {
    val session = session(assessment, state = EXPIRED, finishedAt = NOW)
    facultySession(session)
    now = NOW.plusSeconds(600)

    val reopened = service.reopenSession(assessmentId, "session-1", SessionTimeChangeRequest(15, "  Queda de energia "))

    assertEquals(IN_PROGRESS, reopened.state)
    assertNull(reopened.finishedAt)
    assertEquals(now.plus(Duration.ofMinutes(15)).toString(), reopened.expiresAt)
    assertEquals(now.toString(), reopened.lastHeartbeatAt)
    assertEquals("+15 min: Queda de energia", events.single().detail)
    assertEquals("faculty-a", events.single().actorProfileId)
  }

  @Test
  fun `extending moves the expiry of a running time-limited session`() {
    val session = session(assessment, expiresAt = NOW.plusSeconds(60))
    facultySession(session)

    val extended = service.extendSession(assessmentId, "session-1", SessionTimeChangeRequest(10, "   "))

    assertEquals(NOW.plusSeconds(660).toString(), extended.expiresAt)
    assertEquals("+10 min", events.single().detail)

    session.expiresAt = null
    assertThrows<ConflictException> { service.extendSession(assessmentId, "session-1", SessionTimeChangeRequest(10)) }

    session.state = SUBMITTED
    assertThrows<ConflictException> { service.extendSession(assessmentId, "session-1", SessionTimeChangeRequest(10)) }
  }

  @Test
  fun `closing submits a running session and announces it`() {
    val session = session(assessment)
    facultySession(session)

    assertEquals(SUBMITTED, service.closeSession(assessmentId, "session-1").state)
    assertEquals("CLOSED_BY_FACULTY", ended.single().reason)
    assertEquals("SUBMITTED", ended.single().state)
    assertEquals("faculty-a", events.single().actorProfileId)

    assertThrows<ConflictException> { service.closeSession(assessmentId, "session-1") }
  }

  @Test
  fun `archiving expires the sessions still running`() {
    val running = session(assessment, id = "session-1")
    val submitted = session(assessment, id = "session-2", state = SUBMITTED)
    whenever(sessionRepository.findByAssessmentIdAndState(assessmentId, IN_PROGRESS))
      .thenReturn(listOf(running, submitted, session(assessment, id = "session-3")))
    whenever(sessionRepository.lockById("session-1")).thenReturn(running)
    whenever(sessionRepository.lockById("session-2")).thenReturn(submitted)

    service.finishSessionsOnArchive(assessment)

    assertEquals(EXPIRED, running.state)
    assertEquals(SUBMITTED, submitted.state)
    assertEquals(listOf("CLOSED_BY_ARCHIVE"), ended.map { it.reason })
  }

  @Test
  fun `refreshing stores due expiries of known sessions`() {
    val session = session(assessment, expiresAt = NOW.minusSeconds(1))
    whenever(sessionRepository.lockById("session-1")).thenReturn(session)

    service.refreshSession("missing")
    service.refreshSession("session-1")

    assertEquals(EXPIRED, session.state)
  }

  @Test
  fun `submissions are accepted while running and shortly after the time runs out`() {
    assertThrows<NotFoundException> { service.getSubmissionStatus(assessmentId, "student-b") }

    whenever(sessionRepository.findByAssessmentIdAndStudentProfileId(assessmentId, "student-c")).thenReturn(session(assessment, studentProfileId = "student-c"))
    val untimed = service.getSubmissionStatus(assessmentId, "student-c")
    assertNull(untimed.expiresAt)
    assertTrue(untimed.acceptsSubmissions)

    val session = session(assessment, expiresAt = NOW.plusSeconds(60))
    whenever(sessionRepository.findByAssessmentIdAndStudentProfileId(assessmentId, "student-a")).thenReturn(session)

    assertTrue(service.getSubmissionStatus(assessmentId, "student-a").acceptsSubmissions)

    now = NOW.plusSeconds(64)
    val grace = service.getSubmissionStatus(assessmentId, "student-a")
    assertEquals(EXPIRED, grace.state)
    assertTrue(grace.acceptsSubmissions)
    verify(eventRepository, never()).save(anyValue())

    session.state = IN_PROGRESS
    session.finishedAt = null
    now = NOW.plusSeconds(70)
    assertFalse(service.getSubmissionStatus(assessmentId, "student-a").acceptsSubmissions)
  }

  @Test
  fun `sessions finished in other ways accept no submissions`() {
    val session = session(assessment, state = SUBMITTED, expiresAt = NOW, finishedAt = NOW)
    whenever(sessionRepository.findByAssessmentIdAndStudentProfileId(assessmentId, "student-a")).thenReturn(session)
    assertFalse(service.getSubmissionStatus(assessmentId, "student-a").acceptsSubmissions)

    session.state = EXPIRED
    session.finishedAt = null
    assertFalse(service.getSubmissionStatus(assessmentId, "student-a").acceptsSubmissions)

    session.finishedAt = NOW.minusSeconds(1)
    assertFalse(service.getSubmissionStatus(assessmentId, "student-a").acceptsSubmissions)
  }
}