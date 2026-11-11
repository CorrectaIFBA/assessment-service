package io.correcta.assessment.security

import io.correcta.assessment.exceptions.UnauthorizedException
import io.correcta.assessment.security.annotations.IsLoggedUser
import io.correcta.assessment.security.annotations.IsProfileType
import io.correcta.assessment.security.components.ScopeEvaluatorComponent
import io.correcta.assessment.security.components.aspects.SecurityAspect
import io.correcta.assessment.security.filter.HeaderAuthenticationFilter
import io.correcta.assessment.security.models.IdentityHeaders
import io.correcta.assessment.security.models.ProfilePrincipal
import io.correcta.assessment.support.mock
import io.correcta.assessment.support.whenever
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import java.lang.reflect.Method

class SecurityTest {

  interface Endpoints {
    @IsLoggedUser
    fun loggedIn()

    @IsProfileType("STAFF")
    fun staff()

    fun open()
  }

  interface Other {
    @IsLoggedUser
    fun open(value: String)
  }

  class EndpointsImpl : Endpoints {
    override fun loggedIn() = Unit

    override fun staff() = Unit

    override fun open() = Unit
  }

  private val scopeEvaluator = ScopeEvaluatorComponent()
  private val aspect = SecurityAspect(scopeEvaluator)

  @AfterEach
  fun clearContext() {
    SecurityContextHolder.clearContext()
  }

  private fun authenticate(profileType: String?) {
    SecurityContextHolder.getContext().authentication =
      UsernamePasswordAuthenticationToken(ProfilePrincipal("profile-a", null, profileType, null, null), null, emptyList())
  }

  private fun joinPoint(method: Method, target: Any?): JoinPoint {
    val signature = mock<MethodSignature>()
    whenever(signature.method).thenReturn(method)
    val joinPoint = mock<JoinPoint>()
    whenever(joinPoint.signature).thenReturn(signature)
    whenever(joinPoint.target).thenReturn(target)

    return joinPoint
  }

  private fun implMethod(name: String): Method = EndpointsImpl::class.java.getMethod(name)

  private fun interfaceMethod(name: String): Method = Endpoints::class.java.getMethod(name)

  @Test
  fun `the scope evaluator reads the authenticated profile`() {
    assertFalse(scopeEvaluator.isLoggedUser())
    assertFalse(scopeEvaluator.isProfileType("STAFF"))
    assertNull(scopeEvaluator.currentProfile())

    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("someone", null)
    assertNull(scopeEvaluator.currentProfile())

    authenticate("staff")
    assertTrue(scopeEvaluator.isLoggedUser())
    assertTrue(scopeEvaluator.isProfileType("STAFF"))
    assertFalse(scopeEvaluator.isProfileType("STUDENT"))
  }

  @Test
  fun `annotated endpoints need a logged user`() {
    assertThrows<UnauthorizedException> { aspect.authorize(joinPoint(implMethod("loggedIn"), EndpointsImpl())) }

    authenticate(null)
    assertDoesNotThrow { aspect.authorize(joinPoint(implMethod("loggedIn"), EndpointsImpl())) }
  }

  @Test
  fun `profile type endpoints need that type`() {
    authenticate("STUDENT")
    assertThrows<AuthorizationDeniedException> { aspect.authorize(joinPoint(interfaceMethod("staff"), EndpointsImpl())) }

    authenticate("STAFF")
    assertDoesNotThrow { aspect.authorize(joinPoint(interfaceMethod("staff"), EndpointsImpl())) }
  }

  @Test
  fun `annotations on the controller interface apply`() {
    assertThrows<UnauthorizedException> { aspect.authorize(joinPoint(interfaceMethod("loggedIn"), EndpointsImpl())) }
  }

  @Test
  fun `endpoints without annotations, targets or method signatures pass`() {
    assertDoesNotThrow { aspect.authorize(joinPoint(implMethod("open"), EndpointsImpl())) }
    assertDoesNotThrow { aspect.authorize(joinPoint(interfaceMethod("open"), null)) }
    assertDoesNotThrow { aspect.authorize(joinPoint(Any::class.java.getMethod("hashCode"), EndpointsImpl())) }

    val otherMethod = Other::class.java.getMethod("open", String::class.java)
    assertThrows<UnauthorizedException> { aspect.authorize(joinPoint(otherMethod, EndpointsImpl())) }

    val joinPoint = mock<JoinPoint>()
    whenever(joinPoint.signature).thenReturn(mock<Signature>())
    assertDoesNotThrow { aspect.authorize(joinPoint) }
  }

  @Test
  fun `the header filter authenticates from the identity headers`() {
    val request = MockHttpServletRequest("GET", "/").apply {
      addHeader(IdentityHeaders.PROFILE_ID, " profile-a ")
      addHeader(IdentityHeaders.PERSON_ID, "person-a")
      addHeader(IdentityHeaders.PROFILE_TYPE, "  ")
      addHeader(IdentityHeaders.LOGIN, "login-a")
      addHeader(IdentityHeaders.EMAIL, "a@example.com")
    }

    HeaderAuthenticationFilter().doFilter(request, MockHttpServletResponse(), MockFilterChain())

    assertEquals(ProfilePrincipal("profile-a", "person-a", null, "login-a", "a@example.com"), scopeEvaluator.currentProfile())
  }

  @Test
  fun `the header filter skips anonymous and preflight requests`() {
    HeaderAuthenticationFilter().doFilter(MockHttpServletRequest("GET", "/"), MockHttpServletResponse(), MockFilterChain())
    assertNull(scopeEvaluator.currentProfile())

    val preflight = MockHttpServletRequest("OPTIONS", "/").apply { addHeader(IdentityHeaders.PROFILE_ID, "profile-a") }
    HeaderAuthenticationFilter().doFilter(preflight, MockHttpServletResponse(), MockFilterChain())
    assertNull(scopeEvaluator.currentProfile())
  }
}