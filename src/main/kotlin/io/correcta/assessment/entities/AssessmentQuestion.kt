package io.correcta.assessment.entities

import jakarta.persistence.*
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.Instant

/**
 * Question of an assessment, ordered by [position]. [codeDrafts] holds one starting code and harness per allowed language.
 */
@Entity
@Table(
  name = "assessment_questions",
  indexes = [
    Index(name = "idx_assessment_question_assessment_position", columnList = "assessment_id, position"),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "uk_assessment_question_assessment_position",
      columnNames = ["assessment_id", "position"],
    ),
    UniqueConstraint(
      name = "uk_assessment_question_assessment_slug",
      columnNames = ["assessment_id", "slug"],
    ),
  ],
)
@EntityListeners(AuditingEntityListener::class)
class AssessmentQuestion(
  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  var id: String? = null,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "assessment_id", nullable = false)
  var assessment: Assessment,

  @Column(nullable = false)
  var position: Int,

  @Column(nullable = false, length = 255)
  var title: String,

  @Column(nullable = false, length = 160)
  var slug: String,

  @Column(nullable = false, columnDefinition = "TEXT")
  var statement: String,

  @ElementCollection
  @CollectionTable(
    name = "assessment_question_code_drafts",
    joinColumns = [JoinColumn(name = "question_id", nullable = false)],
    uniqueConstraints = [
      UniqueConstraint(name = "uk_question_code_draft_language", columnNames = ["question_id", "language"]),
    ],
  )
  @BatchSize(size = 50)
  var codeDrafts: MutableList<CodeDraft> = mutableListOf(),

  @Column(nullable = false)
  var maximumGrade: BigDecimal,

  @Column(nullable = false)
  var allowCopyPasting: Boolean = true,

  @CreatedDate
  @Column(nullable = false, updatable = false)
  var createdAt: Instant? = null,

  @LastModifiedDate
  @Column(nullable = false)
  var updatedAt: Instant? = null,
)