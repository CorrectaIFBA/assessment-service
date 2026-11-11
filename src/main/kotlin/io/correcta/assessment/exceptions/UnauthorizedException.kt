package io.correcta.assessment.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Answered with HTTP 401. */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UnauthorizedException(message: String = "É necessário estar autenticado para acessar este recurso.") : RuntimeException(message)