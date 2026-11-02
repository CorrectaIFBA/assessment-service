package io.correcta.assessment.entities

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(
    name = "assessment_parameters",
    indexes = [
        Index(name = "idx_assessment_parameters_assessment", columnList = "assessment_id"),
    ]
)
@EntityListeners(AuditingEntityListener::class)
class AssessmentParameters(


    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    var id: String? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    var assessment: Assessment,

    @Column(nullable = false)
    var canBeSubmittedAfterDueDate: Boolean = false,

    @Column(nullable = true)
    var maximumTimeAllowed: Int? = null,

    


    @Column(nullable = false)
    var showCompilationErrors: Boolean = true,

    @Column(nullable = false)
    var canViewDefinitiveTestcaseScoringAfterSubmission: Boolean = true,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null,

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant? = null
)
