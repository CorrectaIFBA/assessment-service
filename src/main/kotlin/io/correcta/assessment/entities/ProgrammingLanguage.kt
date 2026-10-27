package io.correcta.assessment.entities

/**
 * languages a question's draft code can be written in.
 * stored by name, so new languages can be appended without migrating existing rows.
 */
enum class ProgrammingLanguage {
    C, PYTHON, JAVA
}
