package io.correcta.assessment.entities

import jakarta.persistence.*
import java.time.Instant
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(
    name = "assessments",
    indexes = [
        Index(name = "idx_assessment_owner_profile", columnList = "owner_profile_id"),
        Index(name = "idx_assessment_cohort", columnList = "cohort_id")
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Assessment(
    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    var id: String? = null,

    /**
     * Identity-service profile ID of the faculty member who created the assessment, for visibility purposes only.
     * Only faculties can manage and delete the assessment.
     * If they are members of the cohort as "FACULTIES" of it, they can manage the assessment.
     * If they are not members of the cohort, they cannot manage the assessment.
     */
    @Column(name = "owner_profile_id", nullable = false, updatable = false)
    var ownerProfileId: String,

    /**
     * Cohort-service cohort ID the assessment belongs to.
     */
    @Column(name = "cohort_id", updatable = false)
    var cohortId: String? = null,

    @Column(nullable = false, length = 255)
    var title: String,

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
    var dueDate: Instant? = null
)
