package io.correcta.assessment.entities

import jakarta.persistence.*
import java.time.Instant
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(
    name = "assessment_parameters_change_log",
    indexes = [
        Index(name = "idx_assessment_parameters_change_log_assessment", columnList = "assessment_id"),
        Index(name = "idx_assessment_parameters_change_log_changer_profile", columnList = "changer_profile_id"),
        Index(name = "idx_assessment_parameters_change_log_changed_at", columnList = "changed_at"),
    ]
)
@EntityListeners(AuditingEntityListener::class)
class AssessmentParameterChangeLog(

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    var id: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    var assessment: Assessment,

    @Column(name = "changer_profile_id", updatable = false)
    var changerProfileId: String,

    @Column(nullable = false, updatable = false)
    var parameterName: String,

    @Column(nullable = false, updatable = false)
    var oldValue: String,

    @Column(nullable = false, updatable = false)
    var newValue: String,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var changedAt: Instant? = null
)
