package io.correcta.assessment.entities

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
class CodeDraft(
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var language: ProgrammingLanguage,

    @Column(nullable = false, columnDefinition = "TEXT")
    var code: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    var harness: String = "",
)
