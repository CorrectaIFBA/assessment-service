package io.correcta.assessment.entities

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * Final grade and general faculty comment of one student in one assessment.
 * Per-question correction belongs to submission-service and evaluation-orchestrator.
 */
@Entity
@Table(
    name = "assessment_results",
    indexes = [
        Index(name = "idx_assessment_result_student_profile", columnList = "student_profile_id")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_assessment_result_assessment_student",
            columnNames = ["assessment_id", "student_profile_id"]
        )
    ]
)
@EntityListeners(AuditingEntityListener::class)
class AssessmentResult(
    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    var id: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    var assessment: Assessment,

    /**
     * Identity-service profile ID of the graded student.
     */
    @Column(name = "student_profile_id", nullable = false, updatable = false)
    var studentProfileId: String,

    @Column(precision = 5, scale = 2)
    var finalScore: BigDecimal? = null,

    @Column(columnDefinition = "TEXT")
    var generalComment: String? = null,

    /**
     * Identity-service profile ID of the faculty member who assigned the final score.
     */
    @Column(name = "graded_by_profile_id")
    var gradedByProfileId: String? = null,

    var gradedAt: Instant? = null,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null,

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant? = null
)
