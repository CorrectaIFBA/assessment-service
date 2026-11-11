package io.correcta.assessment.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/** Enables JPA auditing, which fills the `@CreatedDate` and `@LastModifiedDate` fields. */
@Configuration
@EnableJpaAuditing
class JpaConfig