package io.correcta.assessment.security.annotations

/** Requires a profile of type [profileType], case ignored. Otherwise the request fails with HTTP 403. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IsProfileType(val profileType: String)