package io.correcta.assessment.service

import io.correcta.assessment.entities.*
import io.correcta.assessment.exceptions.ConflictException
import io.correcta.assessment.exceptions.ForbiddenException
import io.correcta.assessment.exceptions.NotFoundException
import io.correcta.assessment.messaging.Outbox
import io.correcta.assessment.messaging.session.SessionEndedMessage
import io.correcta.assessment.messaging.session.SessionMessaging
import io.correcta.assessment.models.request.AssessmentParametersRequest
import io.correcta.assessment.models.request.PaginationRequest
import io.correcta.assessment.models.request.SessionFilterRequest
import io.correcta.assessment.models.request.SessionTimeChangeRequest
import io.correcta.assessment.models.response.*
import io.correcta.assessment.repositories.AssessmentParametersRepository
import io.correcta.assessment.repositories.AssessmentSessionEventRepository
import io.correcta.assessment.repositories.AssessmentSessionRepository
import io.correcta.assessment.utils.PaginationExt.toPageRequest
import io.correcta.assessment.utils.PaginationExt.toPagedResponse
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import java.time.Clock
import java.time.Duration
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class AssessmentSessionService(
    val assessmentSessionRepository: AssessmentSessionRepository,
    val assessmentSessionEventRepository: AssessmentSessionEventRepository,
    val assessmentParametersRepository: AssessmentParametersRepository,
    val assessmentAccessService: AssessmentAccessService,
    val entityManager: EntityManager,
    val clock: Clock,
    val outbox: Outbox,
) {

    @Transactional(noRollbackFor = [ConflictException::class])
    fun getMySession(assessmentId: String): GetAssessmentSessionResponse {
        val (_, membership) = assessmentAccessService.findReadableAssessmentWithMembership(assessmentId)
        val now = clock.instant()
        val session =
            assessmentSessionRepository.lockByAssessmentIdAndStudentProfileId(assessmentId, membership.profileId)
                ?: throw NotFoundException("Você ainda não iniciou esta atividade.")

        refresh(session, now)

        return respond(session, now)
    }

    @Transactional(noRollbackFor = [ConflictException::class])
    fun startSession(assessmentId: String): Pair<GetAssessmentSessionResponse, Boolean> {
        val (assessment, membership) = assessmentAccessService.findReadableAssessmentWithMembership(assessmentId)

        if (membership.isFaculty()) {
            throw ForbiddenException("Docentes não iniciam atividades.")
        }

        if (membership.cohortStatus != ACTIVE_COHORT_STATUS) {
            throw ConflictException("A turma não está ativa.")
        }

        entityManager.refresh(assessment, LockModeType.PESSIMISTIC_WRITE)

        if (assessment.state != AssessmentState.PUBLISHED) {
            throw ConflictException("A atividade não está aberta para entregas.")
        }

        val now = clock.instant()
        val existing =
            assessmentSessionRepository.lockByAssessmentIdAndStudentProfileId(assessmentId, membership.profileId)

        if (existing != null) {
            refresh(existing, now)

            if (existing.state.isFinal) {
                throw ConflictException("Você já finalizou esta atividade.")
            }

            record(existing, AssessmentSessionEventType.RESUMED, now)
            countTime(existing, now)

            return respond(existing, now) to false
        }

        val parameters = assessmentParametersRepository.findByAssessmentId(assessmentId)
            ?: AssessmentParametersRequest().toAssessmentParameters(assessment)

        val closingDueDate = assessment.dueDate?.takeUnless { parameters.canBeSubmittedAfterDueDate }

        if (closingDueDate != null && now >= closingDueDate) {
            throw ConflictException("O prazo da atividade terminou.")
        }

        val session = assessmentSessionRepository.saveAndFlush(
            AssessmentSession(
                assessment = assessment,
                studentProfileId = membership.profileId,
                startedAt = now,
                expiresAt = listOfNotNull(
                    parameters.maximumTimeAllowed?.let { now.plus(Duration.ofMinutes(it.toLong())) },
                    closingDueDate,
                ).minOrNull(),
                lastHeartbeatAt = now,
            )
        )

        record(session, AssessmentSessionEventType.STARTED, now)

        return respond(session, now) to true
    }

    @Transactional(noRollbackFor = [ConflictException::class])
    fun heartbeat(assessmentId: String): GetAssessmentSessionResponse {
        val now = clock.instant()
        val session = lockOwnSession(assessmentId)

        refresh(session, now)

        if (session.state.isFinal) {
            return respond(session, now)
        }

        countTime(session, now)

        return respond(session, now)
    }

    @Transactional
    fun deliverSession(assessmentId: String, studentProfileId: String, deliveredAt: Instant) {
        val session = assessmentSessionRepository.lockByAssessmentIdAndStudentProfileId(assessmentId, studentProfileId)
            ?: throw NotFoundException("Sessão não encontrada.")

        refresh(session, maxOf(deliveredAt, session.startedAt))

        if (session.state.isFinal) {
            return
        }

        finish(session, AssessmentSessionState.SUBMITTED, deliveredAt, AssessmentSessionEventType.SUBMITTED)
    }

    @Transactional(readOnly = true)
    fun getMySessions(
        filter: SessionFilterRequest,
        pagination: PaginationRequest
    ): PagedResponse<GetAssessmentSessionResponse> {
        val profileId = assessmentAccessService.getAuthenticatedProfile().profileId
        val pageRequest = pagination.toPageRequest(allowedSortFields = ALLOWED_SORT_FIELDS)
        val now = clock.instant()

        val sessions = filter.state
            ?.let { assessmentSessionRepository.findByStudentProfileIdAndState(profileId, it, pageRequest) }
            ?: assessmentSessionRepository.findByStudentProfileId(profileId, pageRequest)

        return sessions.toPagedResponse(pagination) { session ->
            project(session, now)
            respond(session, now)
        }
    }

    fun summarizeMySessions(assessmentIds: List<String>): Map<String, GetAssessmentSessionSummaryResponse> {
        if (assessmentIds.isEmpty()) return emptyMap()

        val now = clock.instant()

        return assessmentSessionRepository
            .findByAssessmentIdInAndStudentProfileId(
                assessmentIds,
                assessmentAccessService.getAuthenticatedProfile().profileId
            )
            .associate { session ->
                project(session, now)
                session.assessment.id!! to GetAssessmentSessionSummaryResponse.fromSession(session, now)
            }
    }

    @Transactional(readOnly = true)
    fun getSessions(
        assessmentId: String,
        filter: SessionFilterRequest,
        pagination: PaginationRequest
    ): PagedResponse<GetAssessmentSessionResponse> {
        requireFacultyReader(assessmentId)

        val pageRequest = pagination.toPageRequest(allowedSortFields = ALLOWED_SORT_FIELDS)
        val now = clock.instant()

        val sessions = filter.state
            ?.let { assessmentSessionRepository.findByAssessmentIdAndState(assessmentId, it, pageRequest) }
            ?: assessmentSessionRepository.findByAssessmentId(assessmentId, pageRequest)

        val canView = canViewScoring(assessmentId)

        return sessions.toPagedResponse(pagination) { session ->
            project(session, now)
            GetAssessmentSessionResponse.fromSession(session, now, canView)
        }
    }

    @Transactional(readOnly = true)
    fun countSessions(assessmentId: String): Map<AssessmentSessionState, Long> {
        requireFacultyReader(assessmentId)

        val counts = AssessmentSessionState.entries.associateWith { 0L }.toMutableMap()
        assessmentSessionRepository.countByStateOfAssessment(assessmentId).forEach { counts[it.state] = it.total }

        val now = clock.instant()

        assessmentSessionRepository.findByAssessmentIdAndState(assessmentId, AssessmentSessionState.IN_PROGRESS)
            .forEach { session ->
                project(session, now)
                if (session.state.isFinal) {
                    counts.merge(AssessmentSessionState.IN_PROGRESS, -1L, Long::plus)
                    counts.merge(session.state, 1L, Long::plus)
                }
            }

        return counts
    }

    @Transactional(readOnly = true)
    fun getSession(assessmentId: String, sessionId: String): GetAssessmentSessionResponse {
        requireFacultyReader(assessmentId)

        val now = clock.instant()
        val session = findSession(assessmentId, sessionId)

        project(session, now)

        return respond(session, now)
    }

    @Transactional(readOnly = true)
    fun getSessionEvents(assessmentId: String, sessionId: String): List<GetAssessmentSessionEventResponse> {
        requireFacultyReader(assessmentId)
        findSession(assessmentId, sessionId)

        return assessmentSessionEventRepository
            .findBySessionIdOrderByOccurredAtAsc(sessionId)
            .map(GetAssessmentSessionEventResponse::fromEvent)
    }

    @Transactional
    fun reopenSession(
        assessmentId: String,
        sessionId: String,
        request: SessionTimeChangeRequest
    ): GetAssessmentSessionResponse {
        val assessment = assessmentAccessService.findManageableAssessment(assessmentId)
        val actor = assessmentAccessService.getAuthenticatedProfile().profileId
        val now = clock.instant()

        if (assessment.state != AssessmentState.PUBLISHED) {
            throw ConflictException("Publique a atividade para reabrir sessões.")
        }

        val session = lockSession(assessmentId, sessionId)
        refresh(session, now)

        if (!session.state.isFinal) {
            throw ConflictException("A sessão ainda está em andamento.")
        }

        session.state = AssessmentSessionState.IN_PROGRESS
        session.finishedAt = null
        session.expiresAt = now.plus(Duration.ofMinutes(request.extraMinutes!!.toLong()))
        session.lastHeartbeatAt = now

        record(session, AssessmentSessionEventType.REOPENED, now, actor, timeChangeDetail(request))

        return respond(assessmentSessionRepository.saveAndFlush(session), now)
    }

    @Transactional
    fun extendSession(
        assessmentId: String,
        sessionId: String,
        request: SessionTimeChangeRequest
    ): GetAssessmentSessionResponse {
        assessmentAccessService.findManageableAssessment(assessmentId)
        val actor = assessmentAccessService.getAuthenticatedProfile().profileId
        val now = clock.instant()

        val session = lockSession(assessmentId, sessionId)
        refresh(session, now)

        if (session.state.isFinal) {
            throw ConflictException("A sessão já foi finalizada. Reabra-a para dar mais tempo.")
        }

        val expiresAt = session.expiresAt
            ?: throw ConflictException("A sessão não tem limite de tempo.")

        session.expiresAt = expiresAt.plus(Duration.ofMinutes(request.extraMinutes!!.toLong()))
        record(session, AssessmentSessionEventType.EXTENDED, now, actor, timeChangeDetail(request))

        return respond(assessmentSessionRepository.saveAndFlush(session), now)
    }

    @Transactional
    fun closeSession(assessmentId: String, sessionId: String): GetAssessmentSessionResponse {
        assessmentAccessService.findManageableAssessment(assessmentId)
        val actor = assessmentAccessService.getAuthenticatedProfile().profileId
        val now = clock.instant()

        val session = lockSession(assessmentId, sessionId)
        refresh(session, now)

        if (session.state.isFinal) {
            throw ConflictException("A sessão já foi finalizada.")
        }

        finish(session, AssessmentSessionState.SUBMITTED, now, AssessmentSessionEventType.CLOSED_BY_FACULTY, actor)

        return respond(assessmentSessionRepository.saveAndFlush(session), now)
    }

    fun finishSessionsOnArchive(assessment: Assessment) {
        val now = clock.instant()

        assessmentSessionRepository.findByAssessmentIdAndState(assessment.id!!, AssessmentSessionState.IN_PROGRESS)
            .forEach {
                val session = assessmentSessionRepository.lockById(it.id!!) ?: return@forEach

                refresh(session, now)

                if (!session.state.isFinal) {
                    finish(session, AssessmentSessionState.EXPIRED, now, AssessmentSessionEventType.CLOSED_BY_ARCHIVE)
                }
            }
    }

    @Transactional
    fun refreshSession(sessionId: String) {
        val session = assessmentSessionRepository.lockById(sessionId) ?: return

        refresh(session, clock.instant())
    }

    @Transactional(readOnly = true)
    fun getSubmissionStatus(assessmentId: String, studentProfileId: String): GetSessionSubmissionStatusResponse {
        val now = clock.instant()
        val session = assessmentSessionRepository.findByAssessmentIdAndStudentProfileId(assessmentId, studentProfileId)
            ?: throw NotFoundException("Sessão não encontrada.")

        project(session, now)

        val finishedAt = session.finishedAt
        val withinGrace = session.state == AssessmentSessionState.EXPIRED &&
                finishedAt != null && finishedAt == session.expiresAt &&
                !now.isAfter(finishedAt.plus(SUBMISSION_GRACE))

        return GetSessionSubmissionStatusResponse(
            sessionId = session.id!!,
            state = session.state,
            expiresAt = session.expiresAt?.toString(),
            finishedAt = finishedAt?.toString(),
            acceptsSubmissions = session.state == AssessmentSessionState.IN_PROGRESS || withinGrace,
            late = session.isLate(now),
        )
    }

    private fun refresh(session: AssessmentSession, now: Instant) {
        val at = pendingExpiry(session, now) ?: return

        finish(session, AssessmentSessionState.EXPIRED, at, AssessmentSessionEventType.EXPIRED)
    }

    private fun project(session: AssessmentSession, now: Instant) {
        val at = pendingExpiry(session, now) ?: return

        session.state = AssessmentSessionState.EXPIRED
        session.finishedAt = at
    }

    private fun pendingExpiry(session: AssessmentSession, now: Instant): Instant? {
        if (session.state.isFinal) return null

        return session.expiresAt?.takeIf { now > it }
    }

    private fun finish(
        session: AssessmentSession,
        state: AssessmentSessionState,
        at: Instant,
        eventType: AssessmentSessionEventType,
        actorProfileId: String? = null,
    ) {
        session.state = state
        session.finishedAt = at

        record(session, eventType, at, actorProfileId)

        if (eventType != AssessmentSessionEventType.SUBMITTED) {
            outbox.enqueue(
                SessionMessaging.EXCHANGE,
                SessionMessaging.SESSION_ENDED_KEY,
                SessionEndedMessage(
                    sessionId = session.id!!,
                    assessmentId = session.assessment.id!!,
                    studentProfileId = session.studentProfileId,
                    state = state.name,
                    reason = eventType.name,
                    endedAt = at.toString(),
                )
            )
        }
    }

    private fun countTime(session: AssessmentSession, now: Instant) {
        val gap = Duration.between(session.lastHeartbeatAt ?: session.startedAt, now)

        if (gap <= COUNTED_GAP) {
            session.openSeconds += gap.seconds.toInt().coerceAtLeast(0)
        }

        session.lastHeartbeatAt = now
    }

    private fun record(
        session: AssessmentSession,
        type: AssessmentSessionEventType,
        at: Instant,
        actorProfileId: String? = null,
        detail: String? = null,
    ) {
        assessmentSessionEventRepository.save(
            AssessmentSessionEvent(
                session = session,
                type = type,
                occurredAt = at,
                receivedAt = clock.instant(),
                actorProfileId = actorProfileId,
                detail = detail,
            )
        )
    }

    private fun timeChangeDetail(request: SessionTimeChangeRequest): String {
        val reason = request.reason?.trim()?.takeIf(String::isNotEmpty)

        return listOfNotNull("+${request.extraMinutes} min", reason).joinToString(": ")
    }

    private fun lockOwnSession(assessmentId: String): AssessmentSession {
        val (_, membership) = assessmentAccessService.findReadableAssessmentWithMembership(assessmentId)

        return assessmentSessionRepository.lockByAssessmentIdAndStudentProfileId(assessmentId, membership.profileId)
            ?: throw NotFoundException("Você ainda não iniciou esta atividade.")
    }

    private fun lockSession(assessmentId: String, sessionId: String): AssessmentSession {
        return assessmentSessionRepository.lockByIdAndAssessmentId(sessionId, assessmentId)
            ?: throw NotFoundException("Sessão não encontrada.")
    }

    private fun findSession(assessmentId: String, sessionId: String): AssessmentSession {
        return assessmentSessionRepository.findByIdAndAssessmentId(sessionId, assessmentId)
            ?: throw NotFoundException("Sessão não encontrada.")
    }

    private fun requireFacultyReader(assessmentId: String) {
        val (_, membership) = assessmentAccessService.findReadableAssessmentWithMembership(assessmentId)

        if (!membership.isFaculty()) {
            throw ForbiddenException("Apenas docentes da turma podem acompanhar as sessões.")
        }
    }

    private fun respond(session: AssessmentSession, now: Instant): GetAssessmentSessionResponse {
        return GetAssessmentSessionResponse.fromSession(session, now, canViewScoring(session.assessment.id!!))
    }

    private fun canViewScoring(assessmentId: String): Boolean {
        return assessmentParametersRepository.findByAssessmentId(assessmentId)
            ?.canViewDefinitiveTestcaseScoringAfterSubmission
            ?: true
    }

    companion object {
        private const val ACTIVE_COHORT_STATUS = "ACTIVE"
        private val ALLOWED_SORT_FIELDS = setOf("startedAt", "finishedAt", "createdAt")

        private val COUNTED_GAP: Duration = Duration.ofSeconds(60)
        private val SUBMISSION_GRACE: Duration = Duration.ofSeconds(5)
    }
}
