package io.correcta.assessment.support

import io.correcta.assessment.entities.Assessment
import io.correcta.assessment.entities.AssessmentParameters
import io.correcta.assessment.entities.AssessmentQuestion
import io.correcta.assessment.entities.AssessmentQuestionTestcase
import io.correcta.assessment.entities.AssessmentQuestionTestcaseType
import io.correcta.assessment.entities.AssessmentSession
import io.correcta.assessment.entities.AssessmentSessionState
import io.correcta.assessment.entities.AssessmentState
import io.correcta.assessment.entities.CodeDraft
import io.correcta.assessment.entities.ProgrammingLanguage
import io.correcta.assessment.models.response.clients.CohortMembershipResponse
import io.correcta.assessment.security.models.ProfilePrincipal
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.stubbing.OngoingStubbing
import java.math.BigDecimal
import java.time.Instant

/** Builders of saved entities and helpers to use Mockito from Kotlin. */
object Fixtures {
  val NOW: Instant = Instant.parse("2030-03-10T13:00:00Z")

  fun membership(
    role: String = "FACULTY",
    status: String = "ACTIVE",
    profileId: String = if (role == "FACULTY") "faculty-a" else "student-a",
    cohortId: String = "cohort-1",
  ) = CohortMembershipResponse(cohortId, status, profileId, role)

  fun principal(profileId: String = "faculty-a") = ProfilePrincipal(profileId, null, null, null, null)

  fun assessment(
    id: String = "assessment-1",
    state: AssessmentState = AssessmentState.DRAFT,
    cohortId: String? = "cohort-1",
    dueDate: Instant? = null,
  ) = Assessment(
    id = id,
    ownerProfileId = "faculty-a",
    cohortId = cohortId,
    title = "Lista 1",
    slug = "lista-1",
    state = state,
    createdAt = NOW,
    updatedAt = NOW,
    dueDate = dueDate,
  )

  fun parameters(
    assessment: Assessment,
    id: String? = "parameters-1",
    canBeSubmittedAfterDueDate: Boolean = false,
    maximumTimeAllowed: Int? = null,
    canViewScoring: Boolean = true,
  ) = AssessmentParameters(
    id = id,
    assessment = assessment,
    canBeSubmittedAfterDueDate = canBeSubmittedAfterDueDate,
    maximumTimeAllowed = maximumTimeAllowed,
    canViewDefinitiveTestcaseScoringAfterSubmission = canViewScoring,
    createdAt = NOW,
    updatedAt = NOW,
  )

  fun question(
    assessment: Assessment,
    id: String = "question-1",
    position: Int = 0,
    slug: String = "soma",
    maximumGrade: BigDecimal = BigDecimal("2.5"),
    codeDrafts: MutableList<CodeDraft> = mutableListOf(CodeDraft(ProgrammingLanguage.C, "int main() {}", "harness")),
  ) = AssessmentQuestion(
    id = id,
    assessment = assessment,
    position = position,
    title = "Soma",
    slug = slug,
    statement = "Some dois números.",
    codeDrafts = codeDrafts,
    maximumGrade = maximumGrade,
    createdAt = NOW,
    updatedAt = NOW,
  )

  fun testcase(
    question: AssessmentQuestion,
    id: String = "testcase-1",
    position: Int = 1,
    type: AssessmentQuestionTestcaseType = AssessmentQuestionTestcaseType.PUBLIC,
  ) = AssessmentQuestionTestcase(
    id = id,
    question = question,
    position = position,
    input = "1 2",
    expectedOutput = "3",
    type = type,
    createdAt = NOW,
    updatedAt = NOW,
  )

  fun session(
    assessment: Assessment,
    id: String = "session-1",
    studentProfileId: String = "student-a",
    state: AssessmentSessionState = AssessmentSessionState.IN_PROGRESS,
    startedAt: Instant = NOW,
    expiresAt: Instant? = null,
    lastHeartbeatAt: Instant? = NOW,
    finishedAt: Instant? = null,
  ) = AssessmentSession(
    id = id,
    assessment = assessment,
    studentProfileId = studentProfileId,
    state = state,
    createdAt = NOW,
    updatedAt = NOW,
    startedAt = startedAt,
    expiresAt = expiresAt,
    lastHeartbeatAt = lastHeartbeatAt,
    finishedAt = finishedAt,
  )
}

/** Mockito `any()` that Kotlin accepts for non-null parameters. */
@Suppress("UNCHECKED_CAST")
fun <T> anyValue(): T = ArgumentMatchers.any<T>() as T

/** Mockito `eq()` that Kotlin accepts for non-null parameters. */
fun <T> eqValue(value: T): T = ArgumentMatchers.eq(value) ?: value

fun <T> whenever(call: T): OngoingStubbing<T> = Mockito.`when`(call)

inline fun <reified T> mock(): T = Mockito.mock(T::class.java)

/** Stubs [call] to return its first argument, after [prepare] fills the generated fields. */
@Suppress("UNCHECKED_CAST")
fun <T> OngoingStubbing<T>.thenReturnFirstArgument(prepare: (T) -> Unit = {}): OngoingStubbing<T> = thenAnswer { invocation ->
  (invocation.arguments[0] as T).also(prepare)
}