package io.correcta.assessment.utils

import java.text.Normalizer

object SlugExt {

  const val SLUG_PATTERN = "^[a-z0-9]+(-[a-z0-9]+)*$"
  const val SLUG_MAX_LENGTH = 160

  private const val MAX_BASE_LENGTH = 150

  fun String.toSlug(fallback: String): String {
    val slug = Normalizer.normalize(this, Normalizer.Form.NFD)
      .replace(Regex("\\p{M}+"), "")
      .lowercase()
      .replace(Regex("[^a-z0-9]+"), "-")
      .take(MAX_BASE_LENGTH)
      .trim('-')

    return slug.ifEmpty { fallback }
  }

  fun uniqueSlug(base: String, isTaken: (String) -> Boolean): String = generateSequence(1) { it + 1 }
    .map { if (it == 1) base else "$base-$it" }
    .first { !isTaken(it) }
}