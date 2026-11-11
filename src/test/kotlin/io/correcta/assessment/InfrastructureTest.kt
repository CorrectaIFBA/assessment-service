package io.correcta.assessment

import io.correcta.assessment.components.VaryHeaderSuppressingFilter
import io.correcta.assessment.entities.Assessment
import io.correcta.assessment.entities.AssessmentParameterChangeLog
import io.correcta.assessment.entities.AssessmentQuestionTestcaseType
import io.correcta.assessment.entities.AssessmentState
import io.correcta.assessment.entities.CodeDraft
import io.correcta.assessment.entities.ProgrammingLanguage
import io.correcta.assessment.exceptions.BadRequestException
import io.correcta.assessment.messaging.OutboxMessage
import io.correcta.assessment.models.request.PaginationRequest
import io.correcta.assessment.models.response.clients.CohortMembershipResponse
import io.correcta.assessment.repositories.specifications.AssessmentSpecifications
import io.correcta.assessment.repositories.specifications.SpecificationExt.toContainsPattern
import io.correcta.assessment.security.models.ProfilePrincipal
import io.correcta.assessment.support.Fixtures.NOW
import io.correcta.assessment.support.Fixtures.assessment
import io.correcta.assessment.support.Fixtures.parameters
import io.correcta.assessment.support.Fixtures.question
import io.correcta.assessment.support.Fixtures.session
import io.correcta.assessment.support.Fixtures.testcase
import io.correcta.assessment.support.anyValue
import io.correcta.assessment.support.mock
import io.correcta.assessment.support.whenever
import io.correcta.assessment.utils.PaginationExt.toPageRequest
import io.correcta.assessment.utils.SlugExt.toSlug
import io.correcta.assessment.utils.SlugExt.uniqueSlug
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mockStatic
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import java.math.BigDecimal

class InfrastructureTest {

  @Test
  fun `main runs the Spring application`() {
    mockStatic(SpringApplication::class.java).use { springApplication ->
      springApplication.`when`<ConfigurableApplicationContext> { SpringApplication.run(AssessmentServiceApplication::class.java, "--flag") }
        .thenReturn(mock<ConfigurableApplicationContext>())

      main(arrayOf("--flag"))

      springApplication.verify { SpringApplication.run(AssessmentServiceApplication::class.java, "--flag") }
    }
  }

  @Test
  fun `the Vary header never reaches the client`() {
    val response = MockHttpServletResponse()

    VaryHeaderSuppressingFilter().doFilter(MockHttpServletRequest(), response) { _, wrapped ->
      wrapped as jakarta.servlet.http.HttpServletResponse
      wrapped.addHeader("Vary", "Origin")
      wrapped.setHeader("vary", "Accept")
      wrapped.addHeader("X-Kept", "1")
      wrapped.setHeader("X-Set", "2")
      response.addHeader("Vary", "Direct")
      assertEquals(listOf("X-Kept", "X-Set"), wrapped.headerNames.toList())
    }

    assertEquals("1", response.getHeader("X-Kept"))
    assertEquals("2", response.getHeader("X-Set"))
    assertEquals(listOf("Direct"), response.getHeaders("Vary"))
    assertTrue(VaryHeaderSuppressingFilter() is OncePerRequestFilter)
  }

  @Test
  fun `slugs drop accents and symbols and fall back when empty`() {
    assertEquals("introducao-a-c", "  Introdução à C!! ".toSlug("x"))
    assertEquals("x", "!!!".toSlug("x"))
    assertEquals("a".repeat(150), "a".repeat(200).toSlug("x"))
    assertEquals("base-3", uniqueSlug("base") { it != "base-3" })
  }

  @Test
  fun `pagination only sorts by allowed fields`() {
    val pageRequest = PaginationRequest(page = 1, size = 5, sortBy = "title", sortDirection = "ASC").toPageRequest(setOf("title"))

    assertEquals(listOf(1, 5), listOf(pageRequest.pageNumber, pageRequest.pageSize))
    assertTrue(pageRequest.sort.getOrderFor("title")!!.isAscending)
    assertThrows<BadRequestException> { PaginationRequest(sortBy = "secret").toPageRequest(setOf("title")) }
  }

  @Test
  @Suppress("UNCHECKED_CAST")
  fun `specifications filter only by the given values`() {
    assertNull(AssessmentSpecifications.isOfCohort(null))
    assertNull(AssessmentSpecifications.isOfCohort(""))
    assertNull(AssessmentSpecifications.isOfCohort("  "))
    assertNull(AssessmentSpecifications.hasState(null))
    assertNull(AssessmentSpecifications.titleContains(null))
    assertNull(AssessmentSpecifications.titleContains(" "))

    val root = mock<Root<Assessment>>()
    val path = mock<Path<Any>>()
    val builder = mock<CriteriaBuilder>()
    val query = mock<CriteriaQuery<*>>()
    val predicate = mock<Predicate>()
    val lower = mock<Expression<String>>()
    whenever(root.get<Any>(anyValue<String>())).thenReturn(path)
    whenever(builder.equal(anyValue(), anyValue<Any>())).thenReturn(predicate)
    whenever(builder.lower(anyValue())).thenReturn(lower)
    whenever(builder.like(lower, "%a\\%b\\_c\\\\%", '\\')).thenReturn(predicate)

    listOf(
      AssessmentSpecifications.isOfCohort("cohort-1"),
      AssessmentSpecifications.hasState(AssessmentState.DRAFT),
      AssessmentSpecifications.titleContains(" A%b_C\\ "),
    ).forEach { assertEquals(predicate, it!!.toPredicate(root, query, builder)) }

    assertEquals("%x%", "X".toContainsPattern())
  }

  @Test
  fun `entities keep every field`() {
    val assessment = assessment()
    assessment.description = "d"
    assessment.ownerProfileId = "o"
    assessment.cohortId = "c"
    assertEquals(listOf<Any?>("d", "o", "c", NOW), listOf(assessment.description, assessment.ownerProfileId, assessment.cohortId, assessment.createdAt))

    val parameters = parameters(assessment)
    parameters.assessment = assessment
    parameters.showCompilationErrors = false
    assertFalse(parameters.showCompilationErrors)

    val question = question(assessment)
    question.assessment = assessment
    question.allowCopyPasting = false
    assertEquals(listOf<Any?>(false, NOW, NOW), listOf(question.allowCopyPasting, question.createdAt, question.updatedAt))

    val testcase = testcase(question)
    testcase.question = question
    testcase.input = "i"
    testcase.expectedOutput = "o"
    assertEquals(listOf<Any?>("i", "o", NOW), listOf(testcase.input, testcase.expectedOutput, testcase.createdAt))
    assertEquals(
      listOf(listOf(true, true, true), listOf(true, false, false), listOf(false, false, false)),
      AssessmentQuestionTestcaseType.entries.map { listOf(it.isListVisible, it.isInputVisible, it.isExpectedOutputVisible) },
    )

    val session = session(assessment)
    session.assessment = assessment
    session.updatedAt = NOW
    assertEquals(listOf<Any?>(NOW, NOW), listOf(session.createdAt, session.updatedAt))

    val draft = CodeDraft(ProgrammingLanguage.C)
    draft.language = ProgrammingLanguage.C
    draft.code = "c"
    draft.harness = "h"
    assertEquals(listOf<Any>(ProgrammingLanguage.C, "c", "h"), listOf(draft.language, draft.code, draft.harness))

    val log = AssessmentParameterChangeLog(assessment = assessment, changerProfileId = "f", parameterName = "p", oldValue = "o", newValue = "n")
    log.assessment = assessment
    log.changerProfileId = "g"
    log.parameterName = "q"
    log.oldValue = "1"
    log.newValue = "2"
    assertEquals(listOf<Any?>(null, "g", "q", "1", "2", null), listOf(log.id, log.changerProfileId, log.parameterName, log.oldValue, log.newValue, log.changedAt))

    val message = OutboxMessage(exchange = "e", routingKey = "k", payload = "p", createdAt = NOW, availableAt = NOW)
    message.id = "m"
    message.exchange = "e2"
    message.routingKey = "k2"
    message.payload = "p2"
    message.createdAt = NOW
    message.availableAt = NOW
    message.publishedAt = NOW
    message.attempts = 1
    message.lastError = "x"
    assertEquals(listOf<Any?>("m", "e2", "k2", "p2", NOW, NOW, NOW, 1, "x"), listOf(message.id, message.exchange, message.routingKey, message.payload, message.createdAt, message.availableAt, message.publishedAt, message.attempts, message.lastError))

    assertEquals(BigDecimal("2.5"), question.maximumGrade)
    assertNotNull(CohortMembershipResponse("c", "ACTIVE", "p", "FACULTY").cohortId)
    assertEquals(listOf<Any?>("pe", "t", "l", "e"), ProfilePrincipal("p", "pe", "t", "l", "e").let { listOf(it.personId, it.profileType, it.login, it.email) })
  }
}