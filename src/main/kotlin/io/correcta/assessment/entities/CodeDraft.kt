package io.correcta.assessment.entities

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

/**
 * Starting [code] given to the student and hidden [harness] that runs the test cases, for one [language].
 */
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