package io.correcta.assessment.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(
  name = "assessment_question_testcases",
  indexes = [
    Index(
      name = "idx_assessment_question_testcase_question_position",
      columnList = "assessment_question_id, position",
    ),
  ],
  uniqueConstraints = [
    UniqueConstraint(
      name = "uk_assessment_question_testcase_question_position",
      columnNames = ["assessment_question_id", "position"],
    ),
  ],
)
@EntityListeners(AuditingEntityListener::class)
class AssessmentQuestionTestcase(
  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  var id: String? = null,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "assessment_question_id", nullable = false)
  var question: AssessmentQuestion,

  @Column(nullable = false)
  var position: Int,

  @Column(nullable = false, columnDefinition = "TEXT")
  var input: String,

  @Column(nullable = false, columnDefinition = "TEXT")
  var expectedOutput: String,

  @Column(nullable = false)
  var timeLimitSeconds: Int = 5,

  @Column(name = "memory_limit_mb", nullable = false)
  var memoryLimitMB: Int = 256,

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  var type: AssessmentQuestionTestcaseType,

  @CreatedDate
  @Column(nullable = false, updatable = false)
  var createdAt: Instant? = null,

  @LastModifiedDate
  @Column(nullable = false)
  var updatedAt: Instant? = null,
)