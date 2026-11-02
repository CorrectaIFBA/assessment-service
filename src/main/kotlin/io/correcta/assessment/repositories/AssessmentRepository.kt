package io.correcta.assessment.repositories

import io.correcta.assessment.entities.Assessment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface AssessmentRepository : JpaRepository<Assessment, String>, JpaSpecificationExecutor<Assessment> {
    fun findByCohortIdAndSlug(cohortId: String, slug: String): Assessment?

    fun existsByCohortIdAndSlug(cohortId: String, slug: String): Boolean

    fun existsByCohortIdAndSlugAndIdNot(cohortId: String, slug: String, id: String): Boolean
}
