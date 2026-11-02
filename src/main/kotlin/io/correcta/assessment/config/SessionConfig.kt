package io.correcta.assessment.config

import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling


@Configuration
@EnableScheduling
class SessionConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
