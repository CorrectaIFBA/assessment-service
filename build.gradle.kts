plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") version "3.4.12"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.10.4"
}

group = "io.correcta"
version = "0.0.1-SNAPSHOT"
description = "assessment-service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2024.0.0"

dependencies {
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val ktlintOverrides =
    mapOf(
        "ktlint_standard_no-wildcard-imports" to "disabled",
        "ktlint_standard_max-line-length" to "disabled",
        "ktlint_standard_backing-property-naming" to "disabled",
        "ktlint_standard_property-naming" to "disabled",
        "ktlint_standard_function-naming" to "disabled",
        "ktlint_standard_filename" to "disabled",
        "ktlint_standard_package-name" to "disabled",
    )

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.7.1").editorConfigOverride(ktlintOverrides)
    }

    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.7.1").editorConfigOverride(ktlintOverrides)
    }
}

val installGitHooks by tasks.registering(Exec::class) {
    commandLine("git", "config", "core.hooksPath", ".githooks")
    isIgnoreExitValue = true
}

tasks.named("compileKotlin") {
    dependsOn(installGitHooks)
}
