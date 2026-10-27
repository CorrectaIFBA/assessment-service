package io.correcta.assessment.repositories

import io.correcta.assessment.entities.AssessmentParameters
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AssessmentParametersRepository : JpaRepository<AssessmentParameters, String> {
    fun findByAssessmentId(assessmentId: String): AssessmentParameters?

    fun findAllByAssessmentIdIn(assessmentIds: Collection<String>): List<AssessmentParameters>

    fun deleteByAssessmentId(assessmentId: String)
}
