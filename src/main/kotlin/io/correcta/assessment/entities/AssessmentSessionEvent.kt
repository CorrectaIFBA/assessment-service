package io.correcta.assessment.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.time.Instant

/** Audit entry of a session. [actorProfileId] is set when faculty caused the event. */
@Entity
@Table(
  name = "assessment_session_events",
  indexes = [
    Index(name = "idx_assessment_session_events_session", columnList = "session_id, occurred_at"),
  ],
)
class AssessmentSessionEvent(
  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  var id: String? = null,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "session_id", nullable = false)
  var session: AssessmentSession,

  @Column(nullable = false, length = 64)
  @Enumerated(EnumType.STRING)
  var type: AssessmentSessionEventType,

  @Column(nullable = false)
  var occurredAt: Instant,

  @Column(nullable = false)
  var receivedAt: Instant,

  @Column(name = "actor_profile_id")
  var actorProfileId: String? = null,

  @Column(columnDefinition = "TEXT")
  var detail: String? = null,
)