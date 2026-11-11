package io.correcta.assessment.security.models

/** Headers set by the gateway from identity-service `GET /account/me`. */
object IdentityHeaders {
  const val PROFILE_ID = "X-User-Id"
  const val PERSON_ID = "X-User-Person-Id"
  const val PROFILE_TYPE = "X-User-Profile-Type"
  const val LOGIN = "X-User-Login"
  const val EMAIL = "X-User-Email"
}