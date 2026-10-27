package io.correcta.assessment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.EnableAspectJAutoProxy

@ConfigurationPropertiesScan
@EnableAspectJAutoProxy
@EnableFeignClients(basePackages = ["io.correcta.assessment.clients"])
@SpringBootApplication
class AssessmentServiceApplication

fun main(args: Array<String>) {
    runApplication<AssessmentServiceApplication>(*args)
}
