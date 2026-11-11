package io.correcta.assessment.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(
  name = "assessments",
  indexes = [
    Index(name = "idx_assessment_owner_profile", columnList = "owner_profile_id"),
    Index(name = "idx_assessment_cohort", columnList = "cohort_id"),
  ],
  uniqueConstraints = [
    UniqueConstraint(name = "uk_assessment_cohort_slug", columnNames = ["cohort_id", "slug"]),
  ],
)
@EntityListeners(AuditingEntityListener::class)
class Assessment(
  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  var id: String? = null,

  @Column(name = "owner_profile_id", nullable = false, updatable = false)
  var ownerProfileId: String,

  @Column(name = "cohort_id", updatable = false)
  var cohortId: String? = null,

  @Column(nullable = false, length = 255)
  var title: String,

  @Column(nullable = false, length = 160)
  var slug: String,

  @Column(length = 1024)
  var description: String? = null,

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  var state: AssessmentState = AssessmentState.DRAFT,

  @CreatedDate
  @Column(nullable = false, updatable = false)
  var createdAt: Instant? = null,

  @LastModifiedDate
  @Column(nullable = false)
  var updatedAt: Instant? = null,

  @Column
  var dueDate: Instant? = null,
)