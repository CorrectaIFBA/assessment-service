package io.correcta.assessment.security.models

data class ProfilePrincipal(
    val profileId: String,
    val personId: String?,
    val profileType: String?,
    val login: String?,
    val email: String?,
    val traits: List<String>
)
