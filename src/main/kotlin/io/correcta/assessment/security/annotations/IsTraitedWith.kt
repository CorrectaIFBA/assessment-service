package io.correcta.assessment.security.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IsTraitedWith(val trait: String)
