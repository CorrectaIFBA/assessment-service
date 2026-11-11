package io.correcta.assessment.security.components

import io.correcta.assessment.security.models.ProfilePrincipal
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/** Reads the authenticated profile of the current request. */
@Component("scopeEvaluator")
class ScopeEvaluatorComponent {

  /** Whether the request has an authenticated profile. */
  fun isLoggedUser(): Boolean = currentProfile() != null

  /** Whether the authenticated profile has type [profileType], case ignored. */
  fun isProfileType(profileType: String): Boolean = currentProfile()?.profileType.equals(profileType, ignoreCase = true)

  /** Authenticated profile, or null. */
  fun currentProfile(): ProfilePrincipal? = currentAuthentication()?.principal as? ProfilePrincipal

  private fun currentAuthentication(): Authentication? = SecurityContextHolder.getContext().authentication
    ?.takeIf { it.isAuthenticated }
}