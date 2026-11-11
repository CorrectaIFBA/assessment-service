package io.correcta.assessment.models.response.error

/** Error body of a failed validation. [errors] maps each invalid field to its message. */
data class ValidationErrorResponse(
  val message: String,
  val errors: Map<String, String>,
)