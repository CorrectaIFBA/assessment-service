package io.correcta.assessment.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock

/** Enables scheduled jobs (session sweeper and outbox relay) and provides the application clock. */
@Configuration
@EnableScheduling
class SessionConfig {

  /** UTC clock used for every session time. Tests replace this bean to control time. */
  @Bean
  fun clock(): Clock = Clock.systemUTC()
}