package io.correcta.assessment.security.components.aspects

import io.correcta.assessment.exceptions.UnauthorizedException
import io.correcta.assessment.security.annotations.IsLoggedUser
import io.correcta.assessment.security.annotations.IsProfileType
import io.correcta.assessment.security.components.ScopeEvaluatorComponent
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.aop.support.AopUtils
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.stereotype.Component
import java.lang.reflect.Method

/**
 * Enforces [IsLoggedUser] and [IsProfileType] on controller methods.
 *
 * The annotations sit on the controller interfaces, so the aspect also looks at the implemented interfaces.
 */
@Aspect
@Component
class SecurityAspect(
  private val scopeEvaluator: ScopeEvaluatorComponent,
) {

  /** Throws [UnauthorizedException] or `AuthorizationDeniedException` when the caller does not meet the annotations. */
  @Before("execution(* io.correcta.assessment.controllers..*(..))")
  fun authorize(joinPoint: JoinPoint) {
    val isLoggedUser = findSecurityAnnotation<IsLoggedUser>(joinPoint)
    if (isLoggedUser != null && !scopeEvaluator.isLoggedUser()) {
      throw UnauthorizedException("É necessário estar autenticado para acessar este recurso.")
    }

    val isProfileType = findSecurityAnnotation<IsProfileType>(joinPoint)
    if (isProfileType != null && !scopeEvaluator.isProfileType(isProfileType.profileType)) {
      throw AuthorizationDeniedException("Você não tem permissão para acessar este recurso.")
    }
  }

  private inline fun <reified T : Annotation> findSecurityAnnotation(joinPoint: JoinPoint): T? = resolveMethodCandidates(joinPoint)
    .firstNotNullOfOrNull { AnnotatedElementUtils.findMergedAnnotation(it, T::class.java) }

  private fun resolveMethodCandidates(joinPoint: JoinPoint): List<Method> {
    val signature = joinPoint.signature as? MethodSignature ?: return emptyList()
    val method = signature.method
    val targetClass = joinPoint.target?.let { AopUtils.getTargetClass(it) }

    val candidates = linkedSetOf<Method>()
    candidates.add(method)

    if (targetClass != null) {
      candidates.add(AopUtils.getMostSpecificMethod(method, targetClass))
      targetClass.findMatchingMethod(method)?.let(candidates::add)

      targetClass.interfaces
        .mapNotNull { it.findMatchingMethod(method) }
        .forEach(candidates::add)
    }

    method.declaringClass.interfaces
      .mapNotNull { it.findMatchingMethod(method) }
      .forEach(candidates::add)

    return candidates.toList()
  }

  private fun Class<*>.findMatchingMethod(method: Method): Method? = methods.firstOrNull {
    it.name == method.name && it.parameterTypes.contentEquals(method.parameterTypes)
  }
}