package io.correcta.assessment.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(
  name = "assessment_sessions",
  indexes = [
    Index(name = "idx_assessment_session_student_profile", columnList = "student_profile_id"),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "uk_assessment_session_assessment_student",
      columnNames = ["assessment_id", "student_profile_id"],
    ),
  ],
)
@EntityListeners(AuditingEntityListener::class)
class AssessmentSession(
  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  var id: String? = null,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "assessment_id", nullable = false)
  var assessment: Assessment,

  @Column(name = "student_profile_id", nullable = false, updatable = false)
  var studentProfileId: String,

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  var state: AssessmentSessionState = AssessmentSessionState.IN_PROGRESS,

  @CreatedDate
  @Column(nullable = false, updatable = false)
  var createdAt: Instant? = null,

  @LastModifiedDate
  @Column(nullable = false)
  var updatedAt: Instant? = null,

  @Column(nullable = false, updatable = false)
  var startedAt: Instant,

  @Column
  var expiresAt: Instant? = null,

  @Column
  var lastHeartbeatAt: Instant? = null,

  @Column(nullable = false)
  var openSeconds: Int = 0,

  @Column
  var finishedAt: Instant? = null,
)