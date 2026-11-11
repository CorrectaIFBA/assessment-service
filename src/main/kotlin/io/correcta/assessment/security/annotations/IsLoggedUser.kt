package io.correcta.assessment.security.annotations

/** Requires an authenticated profile. Otherwise the request fails with HTTP 401. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IsLoggedUser