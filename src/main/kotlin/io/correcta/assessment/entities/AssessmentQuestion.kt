package io.correcta.assessment.entities

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(
    name = "assessment_questions",
    indexes = [
        Index(name = "idx_assessment_question_assessment_position", columnList = "assessment_id, position")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_assessment_question_assessment_position",
            columnNames = ["assessment_id", "position"]
        )
    ]
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

    @Column(nullable = false, columnDefinition = "TEXT") // markdown or html
    var statement: String,

    /**
     * the languages the student may answer in, each with the starting code handed to them. never empty,
     * and at most one draft per language.
     */
    @ElementCollection
    @CollectionTable(
        name = "assessment_question_code_drafts",
        joinColumns = [JoinColumn(name = "question_id", nullable = false)],
        uniqueConstraints = [
            UniqueConstraint(name = "uk_question_code_draft_language", columnNames = ["question_id", "language"])
        ]
    )
    @BatchSize(size = 50)
    var codeDrafts: MutableList<CodeDraft> = mutableListOf(),

    @Column(nullable = false) // the grade of the assessment is the sum of the maximum grades of all questions
    var maximumGrade: BigDecimal,

    @Column(nullable = false)
    var allowCopyPasting: Boolean = true,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null,

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant? = null
)
