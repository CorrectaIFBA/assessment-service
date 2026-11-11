package io.correcta.assessment.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Answered with HTTP 403. */
@ResponseStatus(HttpStatus.FORBIDDEN)
class ForbiddenException(message: String = "Você não tem permissão para acessar este recurso.") : RuntimeException(message)