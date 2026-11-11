package io.correcta.assessment.repositories

import io.correcta.assessment.entities.AssessmentParameterChangeLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/** Persistence of [AssessmentParameterChangeLog]. */
@Repository
interface AssessmentParameterChangeLogRepository : JpaRepository<AssessmentParameterChangeLog, String>