package io.correcta.assessment.entities

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

/**
 * starting code of a question in one language. a question allows exactly the languages it has a draft for,
 * so the student picks one of them and starts from its code.
 */
@Embeddable
class CodeDraft(
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var language: ProgrammingLanguage,

    @Column(nullable = false, columnDefinition = "TEXT") // may be empty
    var code: String = "",
)
