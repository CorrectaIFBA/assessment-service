package io.correcta.assessment.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Answered with HTTP 500. The message is logged, not returned. */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class InternalServerErrorException(message: String = "Ocorreu um erro inesperado no servidor.") : RuntimeException(message)