package io.correcta.assessment.repositories.specifications

import io.correcta.assessment.entities.Assessment
import io.correcta.assessment.entities.AssessmentState
import io.correcta.assessment.repositories.specifications.SpecificationExt.LIKE_ESCAPE_CHAR
import io.correcta.assessment.repositories.specifications.SpecificationExt.toContainsPattern
import org.springframework.data.jpa.domain.Specification

/** Filters of the assessment list. A function returns null when its filter is not set. */
object AssessmentSpecifications {

  fun isOfCohort(cohortId: String?): Specification<Assessment>? {
    if (cohortId.isNullOrBlank()) return null

    return Specification { root, _, builder ->
      builder.equal(root.get<String>("cohortId"), cohortId)
    }
  }

  fun hasState(state: AssessmentState?): Specification<Assessment>? {
    if (state == null) return null

    return Specification { root, _, builder ->
      builder.equal(root.get<AssessmentState>("state"), state)
    }
  }

  fun titleContains(title: String?): Specification<Assessment>? {
    if (title.isNullOrBlank()) return null

    return Specification { root, _, builder ->
      builder.like(builder.lower(root.get("title")), title.toContainsPattern(), LIKE_ESCAPE_CHAR)
    }
  }
}