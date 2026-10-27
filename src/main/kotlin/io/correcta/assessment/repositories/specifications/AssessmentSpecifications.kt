package io.correcta.assessment.repositories.specifications

import io.correcta.assessment.entities.Assessment
import io.correcta.assessment.entities.AssessmentState
import io.correcta.assessment.repositories.specifications.SpecificationExt.LIKE_ESCAPE_CHAR
import io.correcta.assessment.repositories.specifications.SpecificationExt.toContainsPattern
import org.springframework.data.jpa.domain.Specification

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

    fun ownedBy(ownerProfileId: String): Specification<Assessment> {
        return Specification { root, _, builder ->
            builder.equal(root.get<String>("ownerProfileId"), ownerProfileId)
        }
    }

    fun titleContains(title: String?): Specification<Assessment>? {
        if (title.isNullOrBlank()) return null

        return Specification { root, _, builder ->
            builder.like(builder.lower(root.get("title")), title.toContainsPattern(), LIKE_ESCAPE_CHAR)
        }
    }
}
