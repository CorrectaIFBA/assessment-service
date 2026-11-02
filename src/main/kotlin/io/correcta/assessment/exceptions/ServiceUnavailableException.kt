package io.correcta.assessment.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class ServiceUnavailableException(message: String = "Um serviço necessário está indisponível no momento. Tente novamente mais tarde.") :
    RuntimeException(message)
