package io.correcta.assessment.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.correcta.assessment.exceptions.NotFoundException
import io.correcta.assessment.messaging.session.SessionDeliveredListener
import io.correcta.assessment.messaging.session.SessionEndedMessage
import io.correcta.assessment.messaging.session.SessionMessaging
import io.correcta.assessment.service.AssessmentSessionService
import io.correcta.assessment.support.Fixtures.NOW
import io.correcta.assessment.support.anyValue
import io.correcta.assessment.support.mock
import io.correcta.assessment.support.thenReturnFirstArgument
import io.correcta.assessment.support.whenever
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.springframework.amqp.AmqpException
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageDeliveryMode
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitOperations
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Clock

class MessagingTest {

  private val clock = mock<Clock>().also { whenever(it.instant()).thenReturn(NOW) }

  /** Serves queued batches of pending rows and records every update. */
  private class FakeJdbcTemplate(val batches: ArrayDeque<List<Map<String, Any>>>) : JdbcTemplate() {
    val updates = mutableListOf<List<Any?>>()

    override fun <T : Any?> query(sql: String, rowMapper: RowMapper<T>, vararg args: Any?): List<T> = batches.removeFirstOrNull().orEmpty().mapIndexed { index, row ->
      val resultSet = mock<ResultSet>()
      whenever(resultSet.getString(anyValue<String>())).thenAnswer { row[it.arguments[0] as String] }
      whenever(resultSet.getInt("attempts")).thenReturn(row["attempts"] as Int)
      rowMapper.mapRow(resultSet, index)!!
    }

    override fun update(sql: String, vararg args: Any?): Int {
      updates += listOf(sql, *args)
      return 1
    }
  }

  private fun row(id: String, attempts: Int = 0) = mapOf("id" to id, "exchange" to "x", "routing_key" to "key-$id", "payload" to "{}", "attempts" to attempts)

  private fun relay(jdbc: JdbcTemplate, operations: RabbitOperations, transactionTemplate: TransactionTemplate = runningTransactions()): OutboxRelay {
    val rabbitTemplate = mock<RabbitTemplate>()
    whenever(rabbitTemplate.invoke(anyValue<RabbitOperations.OperationsCallback<Any>>())).thenAnswer {
      (it.arguments[0] as RabbitOperations.OperationsCallback<*>).doInRabbit(operations)
    }

    return OutboxRelay(jdbc, rabbitTemplate, transactionTemplate, clock)
  }

  private fun runningTransactions(): TransactionTemplate = mock<TransactionTemplate>().also {
    whenever(it.execute(anyValue<TransactionCallback<Int>>())).thenAnswer { invocation ->
      (invocation.arguments[0] as TransactionCallback<*>).doInTransaction(mock<TransactionStatus>())
    }
  }

  @Test
  fun `the outbox stores the payload as JSON at the current time`() {
    val repository = mock<OutboxMessageRepository>()
    whenever(repository.save(anyValue<OutboxMessage>())).thenReturnFirstArgument()
    val objectMapper = ObjectMapper()
    val outbox = Outbox(repository, objectMapper, clock)

    outbox.enqueue("exchange", "key", mapOf("a" to 1))

    val captor = ArgumentCaptor.forClass(OutboxMessage::class.java)
    verify(repository).save(captor.capture())
    val message = captor.value
    assertEquals(listOf("exchange", "key", """{"a":1}""", NOW, NOW), listOf(message.exchange, message.routingKey, message.payload, message.createdAt, message.availableAt))
    assertNull(message.id)
    assertNull(message.publishedAt)
    assertNull(message.lastError)
    assertEquals(0, message.attempts)
    assertSame(repository, outbox.outboxMessageRepository)
    assertSame(objectMapper, outbox.objectMapper)
    assertSame(clock, outbox.clock)
  }

  @Test
  fun `the relay publishes full batches until one is not full`() {
    val jdbc = FakeJdbcTemplate(ArrayDeque(listOf((1..50).map { row("m$it") }, listOf(row("m51")))))
    val operations = mock<RabbitOperations>()
    val relay = relay(jdbc, operations)

    relay.relay()

    assertEquals(51, jdbc.updates.size)
    assertEquals(listOf("UPDATE outbox_messages SET published_at = ? WHERE id = ?", Timestamp.from(NOW), "m51"), jdbc.updates.last())
    val captor = ArgumentCaptor.forClass(Message::class.java)
    verify(operations).send(org.mockito.ArgumentMatchers.eq("x"), org.mockito.ArgumentMatchers.eq("key-m51"), captor.capture())
    assertEquals(MessageProperties.CONTENT_TYPE_JSON, captor.value.messageProperties.contentType)
    assertEquals(MessageDeliveryMode.PERSISTENT, captor.value.messageProperties.deliveryMode)
    assertEquals("m51", captor.value.messageProperties.messageId)
    assertSame(jdbc, relay.jdbcTemplate)
    assertSame(clock, relay.clock)
  }

  @Test
  fun `failed messages back off and stop the loop`() {
    val jdbc = FakeJdbcTemplate(ArrayDeque(listOf((1..50).map { row("m$it", attempts = 20) })))
    val operations = mock<RabbitOperations>()
    doThrow(AmqpException("down")).`when`(operations).waitForConfirmsOrDie(5_000L)
    doThrow(AmqpException(null as String?)).`when`(operations).send(org.mockito.ArgumentMatchers.eq("x"), org.mockito.ArgumentMatchers.eq("key-m1"), anyValue())

    relay(jdbc, operations).relay()

    assertEquals(50, jdbc.updates.size)
    assertEquals(listOf("UPDATE outbox_messages SET attempts = ?, last_error = ?, available_at = ? WHERE id = ?", 21, null, Timestamp.from(NOW.plusSeconds(300)), "m1"), jdbc.updates.first())
    assertEquals("down", jdbc.updates.last()[2])
  }

  @Test
  fun `the relay stops when the transaction returns nothing`() {
    val transactionTemplate = mock<TransactionTemplate>()
    val relay = relay(FakeJdbcTemplate(ArrayDeque()), mock(), transactionTemplate)

    assertDoesNotThrow { relay.relay() }
    assertSame(transactionTemplate, relay.transactionTemplate)
    assertNotNull(relay.rabbitTemplate)
  }

  @Test
  fun `deliveries submit the session and unknown sessions are dropped`() {
    val sessionService = mock<AssessmentSessionService>()
    val objectMapper = jacksonObjectMapper()
    val listener = SessionDeliveredListener(sessionService, objectMapper)
    val body = """{"assessmentId": "a", "studentProfileId": "s", "deliveredAt": "2030-03-10T13:00:00Z"}""".toByteArray()

    listener.onSessionDelivered(Message(body))
    verify(sessionService).deliverSession("a", "s", NOW)

    doThrow(NotFoundException()).`when`(sessionService).deliverSession("a", "s", NOW)
    assertDoesNotThrow { listener.onSessionDelivered(Message(body)) }
    assertSame(sessionService, listener.assessmentSessionService)
    assertSame(objectMapper, listener.objectMapper)
  }

  @Test
  fun `session ended messages carry the session and the reason`() {
    val message = SessionEndedMessage("s", "a", "p", "EXPIRED", "CLOSED_BY_ARCHIVE", "t")

    assertEquals(
      listOf("s", "a", "p", "EXPIRED", "CLOSED_BY_ARCHIVE", "t"),
      listOf(message.sessionId, message.assessmentId, message.studentProfileId, message.state, message.reason, message.endedAt),
    )
    assertEquals("correcta.assessment-sessions", SessionMessaging.EXCHANGE)
  }
}