package io.correcta.assessment.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Answered with HTTP 404. */
@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException(message: String = "O recurso solicitado não foi encontrado.") : RuntimeException(message)