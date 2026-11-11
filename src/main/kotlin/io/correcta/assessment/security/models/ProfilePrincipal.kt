package io.correcta.assessment.security.models

/** Authenticated profile, read from the [IdentityHeaders]. */
data class ProfilePrincipal(
  val profileId: String,
  val personId: String?,
  val profileType: String?,
  val login: String?,
  val email: String?,
)