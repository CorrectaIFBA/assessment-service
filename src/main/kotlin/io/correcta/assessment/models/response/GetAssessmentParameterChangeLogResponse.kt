package io.correcta.assessment.models.response

import io.correcta.assessment.entities.AssessmentParameterChangeLog

data class GetAssessmentParameterChangeLogResponse(
  val id: String,
  val assessmentId: String,
  val changerProfileId: String,
  val parameterName: String,
  val oldValue: String,
  val newValue: String,
  val changedAt: String,
) {

  companion object {

    fun fromChangeLog(changeLog: AssessmentParameterChangeLog): GetAssessmentParameterChangeLogResponse = GetAssessmentParameterChangeLogResponse(
      id = changeLog.id!!,
      assessmentId = changeLog.assessment.id!!,
      changerProfileId = changeLog.changerProfileId,
      parameterName = changeLog.parameterName,
      oldValue = changeLog.oldValue,
      newValue = changeLog.newValue,
      changedAt = changeLog.changedAt!!.toString(),
    )
  }
}