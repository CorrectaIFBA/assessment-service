package io.correcta.assessment.models.response

import io.correcta.assessment.entities.AssessmentSessionEvent
import io.correcta.assessment.entities.AssessmentSessionEventType

data class GetAssessmentSessionEventResponse(
  val id: String,
  val type: AssessmentSessionEventType,
  val occurredAt: String,
  val receivedAt: String,
  val actorProfileId: String?,
  val detail: String?,
) {

  companion object {

    fun fromEvent(event: AssessmentSessionEvent): GetAssessmentSessionEventResponse = GetAssessmentSessionEventResponse(
      id = event.id!!,
      type = event.type,
      occurredAt = event.occurredAt.toString(),
      receivedAt = event.receivedAt.toString(),
      actorProfileId = event.actorProfileId,
      detail = event.detail,
    )
  }
}