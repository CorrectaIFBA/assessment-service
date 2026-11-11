package io.correcta.assessment.handlers

import io.correcta.assessment.exceptions.BadRequestException
import io.correcta.assessment.exceptions.ConflictException
import io.correcta.assessment.exceptions.ForbiddenException
import io.correcta.assessment.exceptions.InternalServerErrorException
import io.correcta.assessment.exceptions.NotFoundException
import io.correcta.assessment.exceptions.ServiceUnavailableException
import io.correcta.assessment.exceptions.UnauthorizedException
import io.correcta.assessment.support.mock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.servlet.resource.NoResourceFoundException

class CustomExceptionHandlerTest {

  private val handler = CustomExceptionHandler()

  @Test
  fun `application exceptions keep their status and message`() {
    assertEquals(400 to "bad", handler.handleBadRequestException(BadRequestException("bad")).let { it.statusCode.value() to it.body!!.message })
    assertEquals(401 to "who", handler.handleUnauthorizedException(UnauthorizedException("who")).let { it.statusCode.value() to it.body!!.message })
    assertEquals(403 to "no", handler.handleForbiddenException(ForbiddenException("no")).let { it.statusCode.value() to it.body!!.message })
    assertEquals(404 to "gone", handler.handleNotFoundException(NotFoundException("gone")).let { it.statusCode.value() to it.body!!.message })
    assertEquals(409 to "clash", handler.handleConflictException(ConflictException("clash")).let { it.statusCode.value() to it.body!!.message })
    assertEquals(503 to "down", handler.handleServiceUnavailableException(ServiceUnavailableException("down")).let { it.statusCode.value() to it.body!!.message })
  }

  @Test
  fun `exceptions without a message get the default message`() {
    assertEquals(
      "A requisição não pôde ser compreendida ou não possui os parâmetros obrigatórios.",
      handler.handleBadRequestException(mock<BadRequestException>()).body!!.message,
    )
    assertEquals(
      "É necessário estar autenticado para acessar este recurso.",
      handler.handleUnauthorizedException(mock<UnauthorizedException>()).body!!.message,
    )
    assertEquals("Você não tem permissão para acessar este recurso.", handler.handleForbiddenException(mock<ForbiddenException>()).body!!.message)
    assertEquals("O recurso solicitado não foi encontrado.", handler.handleNotFoundException(mock<NotFoundException>()).body!!.message)
    assertEquals(
      "A requisição não pôde ser concluída devido a um conflito com o estado atual do recurso.",
      handler.handleConflictException(mock<ConflictException>()).body!!.message,
    )
    assertEquals(
      "Um serviço necessário está indisponível no momento. Tente novamente mais tarde.",
      handler.handleServiceUnavailableException(mock<ServiceUnavailableException>()).body!!.message,
    )
  }

  @Test
  fun `the exceptions have default messages`() {
    assertEquals("A requisição não pôde ser compreendida ou não possui os parâmetros obrigatórios.", BadRequestException().message)
    assertEquals("A requisição não pôde ser concluída devido a um conflito com o estado atual do recurso.", ConflictException().message)
    assertEquals("Você não tem permissão para acessar este recurso.", ForbiddenException().message)
    assertEquals("Ocorreu um erro inesperado no servidor.", InternalServerErrorException().message)
    assertEquals("O recurso solicitado não foi encontrado.", NotFoundException().message)
    assertEquals("Um serviço necessário está indisponível no momento. Tente novamente mais tarde.", ServiceUnavailableException().message)
    assertEquals("É necessário estar autenticado para acessar este recurso.", UnauthorizedException().message)
  }

  @Test
  fun `validation errors list each field`() {
    val result = BeanPropertyBindingResult(Any(), "body").apply {
      addError(FieldError("body", "title", "O título é obrigatório"))
      addError(FieldError("body", "slug", null, false, null, null, null))
    }
    val exception = MethodArgumentNotValidException(MethodParameter(Any::class.java.getMethod("toString"), -1), result)

    val response = handler.handleMethodArgumentNotValidException(exception)

    assertEquals(400, response.statusCode.value())
    assertEquals(mapOf("title" to "O título é obrigatório", "slug" to "Valor inválido"), response.body!!.errors)
  }

  @Test
  fun `framework exceptions get their status`() {
    val unreadable = HttpMessageNotReadableException("bad json", MockHttpInputMessage(ByteArray(0)))
    assertEquals(400, handler.handleHttpMessageNotReadableException(unreadable).statusCode.value())

    assertEquals(403, handler.handleAuthorizationDeniedException().statusCode.value())
    assertEquals(404, handler.handleNoResourceFoundException(NoResourceFoundException(HttpMethod.GET, "/x")).statusCode.value())

    val unsupportedType = HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, listOf(MediaType.APPLICATION_JSON))
    assertEquals(
      "O tipo de conteúdo 'text/plain' não é suportado por este endpoint.",
      handler.handleMediaTypeNotSupportedException(unsupportedType).body!!.message,
    )

    assertEquals(500, handler.handleInternalServerErrorException(InternalServerErrorException()).statusCode.value())
    assertEquals(500, handler.handleGenericException(IllegalStateException()).statusCode.value())
  }

  @Test
  fun `unsupported methods list the allowed ones when known`() {
    val withAllowed = handler.handleMethodNotSupportedException(HttpRequestMethodNotSupportedException("PUT", listOf("GET", "POST")))
    assertEquals(405, withAllowed.statusCode.value())
    assertEquals("GET, POST", withAllowed.headers.getFirst("Allow"))
    assertEquals("O método 'PUT' não é suportado por este endpoint.", withAllowed.body!!.message)

    assertNull(handler.handleMethodNotSupportedException(HttpRequestMethodNotSupportedException("PUT")).headers.getFirst("Allow"))
  }
}