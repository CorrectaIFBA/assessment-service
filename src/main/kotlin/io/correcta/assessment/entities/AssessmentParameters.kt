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

    // other possible parameters (matching a coding assessment system):
    //  - can be submitted after due date
    //  - maximum grade
    //  - needs to finish in one sitting (for exams - true or false) - tells if the student can leave the exam and come back to it or not (selecting/alterning)
    //  - maximum time allowed (if not specified, then no time limit, for exams - in minutes) - tells how long the student has to complete the exam (selecting/alterning)
    //  - monitor for cheating (for exams - page focus monitoring)
    //  - can view questions out of order (for exams - true or false) - tells if the student can view the questions out of order or not (selecting/alterning)
    //  - can view definitive testcase scoring after submission (for exams - true or false) - tells if the student can view the final testcase scoring after submission or not (selecting/alterning)
    //  - question order (for exams - fixed or random) - tells if the student can see the questions in a fixed order or random order

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    var id: String? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    var assessment: Assessment,

    @Column(nullable = false)
    var canBeSubmittedAfterDueDate: Boolean = false,

    @Column(nullable = false)
    var needsToFinishInOneSitting: Boolean = false,

    @Column(nullable = true) // in minutes, null means no time limit
    var maximumTimeAllowed: Int? = null,

    @Column(nullable = false)
    var monitorForCheating: Boolean = false,

    @Column(nullable = false)
    var canViewDefinitiveTestcaseScoringAfterSubmission: Boolean = true,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null,

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant? = null
)
