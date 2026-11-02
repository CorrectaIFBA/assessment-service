package io.correcta.assessment.handlers

import io.correcta.assessment.exceptions.*
import io.correcta.assessment.models.response.error.ErrorResponse
import io.correcta.assessment.models.response.error.ValidationErrorResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException


@RestControllerAdvice
class CustomExceptionHandler {

    private val log: Logger = LoggerFactory.getLogger(CustomExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(exception: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
        val fieldErrors = exception.bindingResult.fieldErrors
        val errorList = fieldErrors.stream().map<String?> { obj: FieldError? -> obj!!.defaultMessage }.toList()

        val validationErrorResponse =
            ValidationErrorResponse(
                message = "A validação dos parâmetros da requisição falhou.",
                errors = fieldErrors.associate { it.field to (it.defaultMessage ?: "Valor inválido") },
            )

        log.warn("Validation errors : {} ; Parameters : {}", errorList, exception.bindingResult.target)

        return ResponseEntity.status(400)
            .body(validationErrorResponse)
    }


    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(exception: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        log.warn("Unreadable request body: {}", exception.mostSpecificCause.message)

        return ResponseEntity
            .status(400)
            .body(ErrorResponse("O corpo da requisição é inválido ou contém valores não suportados."))
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequestException(exception: BadRequestException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(400)
            .body(
                ErrorResponse(
                    exception.message
                        ?: "A requisição não pôde ser compreendida ou não possui os parâmetros obrigatórios."
                )
            )
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedException(exception: UnauthorizedException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(401)
            .body(ErrorResponse(exception.message ?: "É necessário estar autenticado para acessar este recurso."))
    }

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(exception: ForbiddenException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(403)
            .body(ErrorResponse(exception.message ?: "Você não tem permissão para acessar este recurso."))
    }

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAuthorizationDeniedException(): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(403)
            .body(ErrorResponse("Você não tem permissão para acessar este recurso."))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(exception: NoResourceFoundException): ResponseEntity<ErrorResponse> {
        log.warn("No handler for {}", exception.resourcePath)

        return ResponseEntity
            .status(404)
            .body(ErrorResponse("O recurso solicitado não foi encontrado."))
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(exception: NotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(404)
            .body(ErrorResponse(exception.message ?: "O recurso solicitado não foi encontrado."))
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupportedException(exception: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> {
        val supported = exception.supportedMethods?.joinToString(", ").orEmpty()

        log.warn("Method {} is not supported for this endpoint (supported: {})", exception.method, supported)

        val response = ResponseEntity.status(405)

        if (supported.isNotEmpty()) response.header("Allow", supported)

        return response.body(ErrorResponse("O método '${exception.method}' não é suportado por este endpoint."))
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(exception: ConflictException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(409)
            .body(
                ErrorResponse(
                    exception.message
                        ?: "A requisição não pôde ser concluída devido a um conflito com o estado atual do recurso."
                )
            )
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupportedException(exception: HttpMediaTypeNotSupportedException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(415)
            .body(ErrorResponse("O tipo de conteúdo '${exception.contentType}' não é suportado por este endpoint."))
    }

    @ExceptionHandler(ServiceUnavailableException::class)
    fun handleServiceUnavailableException(exception: ServiceUnavailableException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(503)
            .body(
                ErrorResponse(
                    exception.message
                        ?: "Um serviço necessário está indisponível no momento. Tente novamente mais tarde."
                )
            )
    }

    @ExceptionHandler(InternalServerErrorException::class)
    fun handleInternalServerErrorException(exception: InternalServerErrorException): ResponseEntity<ErrorResponse> {
        log.error("Internal server error: ", exception)

        return ResponseEntity
            .status(500)
            .body(ErrorResponse("Ocorreu um erro interno no servidor. Por favor, tente novamente mais tarde."))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(exception: Exception): ResponseEntity<ErrorResponse> {
        log.error("An unexpected error occurred: ", exception)

        return ResponseEntity
            .status(500)
            .body(ErrorResponse("Ocorreu um erro inesperado. Por favor, tente novamente mais tarde."))
    }

}