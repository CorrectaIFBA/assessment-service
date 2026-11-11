package io.correcta.assessment.repositories.specifications

/** Helpers for `LIKE` filters. */
object SpecificationExt {

  const val LIKE_ESCAPE_CHAR = '\\'

  /**
   * Lowercase `LIKE` pattern that matches values containing this text. Escapes the backslash, `%` and `_` with [LIKE_ESCAPE_CHAR].
   */
  fun String.toContainsPattern(): String {
    val escaped = trim()
      .lowercase()
      .replace("\\", "\\\\")
      .replace("%", "\\%")
      .replace("_", "\\_")

    return "%$escaped%"
  }
}