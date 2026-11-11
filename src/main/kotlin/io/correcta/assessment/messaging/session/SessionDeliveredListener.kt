package io.correcta.assessment.messaging.session

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.correcta.assessment.exceptions.NotFoundException
import io.correcta.assessment.service.AssessmentSessionService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import java.time.Instant

/** Consumes `session.delivered` messages from workspace-service and marks the sessions submitted. */
@Component
class SessionDeliveredListener(
  val assessmentSessionService: AssessmentSessionService,
  val objectMapper: ObjectMapper,
) {

  private val log: Logger = LoggerFactory.getLogger(SessionDeliveredListener::class.java)

  /** Submits the session of the message. A message for an unknown session is logged and dropped. */
  @RabbitListener(queues = [SessionMessaging.SESSION_DELIVERED_QUEUE])
  fun onSessionDelivered(message: Message) {
    val delivered = objectMapper.readValue<SessionDeliveredMessage>(message.body)

    try {
      assessmentSessionService.deliverSession(
        delivered.assessmentId,
        delivered.studentProfileId,
        Instant.parse(delivered.deliveredAt),
      )
    } catch (exception: NotFoundException) {
      log.warn("Delivery for unknown session: assessment {}, student {}.", delivered.assessmentId, delivered.studentProfileId)
    }
  }
}