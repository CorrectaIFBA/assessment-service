package io.correcta.assessment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.EnableAspectJAutoProxy

/**
 * Entry point of assessment-service.
 *
 * Enables configuration properties scanning, AspectJ proxies (used by the security aspect)
 * and the OpenFeign clients of the `clients` package.
 */
@ConfigurationPropertiesScan
@EnableAspectJAutoProxy
@EnableFeignClients(basePackages = ["io.correcta.assessment.clients"])
@SpringBootApplication
class AssessmentServiceApplication

/** Starts the Spring Boot application. */
fun main(args: Array<String>) {
  runApplication<AssessmentServiceApplication>(*args)
}