package io.correcta.assessment.security.filter

import io.correcta.assessment.security.models.IdentityHeaders
import io.correcta.assessment.security.models.ProfilePrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Builds a [ProfilePrincipal] from the [IdentityHeaders] set by the gateway.
 *
 * The headers are trusted as they are, so the service must only be reachable through the gateway.
 * `OPTIONS` requests are not filtered.
 */
class HeaderAuthenticationFilter : OncePerRequestFilter() {

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    val profileId = request.readHeader(IdentityHeaders.PROFILE_ID)

    if (profileId != null) {
      val principal = ProfilePrincipal(
        profileId = profileId,
        personId = request.readHeader(IdentityHeaders.PERSON_ID),
        profileType = request.readHeader(IdentityHeaders.PROFILE_TYPE),
        login = request.readHeader(IdentityHeaders.LOGIN),
        email = request.readHeader(IdentityHeaders.EMAIL),
      )

      SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(principal, null, emptyList())
    }

    filterChain.doFilter(request, response)
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean = request.method.equals("OPTIONS", ignoreCase = true)

  private fun HttpServletRequest.readHeader(name: String): String? = getHeader(name)?.trim()?.takeIf { it.isNotEmpty() }
}