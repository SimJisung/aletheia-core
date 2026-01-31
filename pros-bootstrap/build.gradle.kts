/**
 * pros-bootstrap: Application Entry Point
 *
 * This module contains:
 * - Spring Boot main application
 * - Configuration files
 * - Environment-specific settings
 *
 * Dependencies: All other modules
 */

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":pros-domain"))
    implementation(project(":pros-application"))
    implementation(project(":pros-infrastructure"))
    implementation(project(":pros-api"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // DevTools (optional, for development)
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Configuration processor
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.bootJar {
    archiveBaseName.set("pros")
    archiveVersion.set(project.version.toString())
}

tasks.bootRun {
    // Set working directory to project root for .env file loading
    workingDir = rootProject.projectDir

    // Load .env file and set environment variables
    doFirst {
        val envFile = rootProject.file(".env")
        if (envFile.exists()) {
            envFile.readLines()
                .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
                .forEach { line ->
                    val (key, value) = line.split("=", limit = 2)
                    environment(key.trim(), value.trim())
                }
        }
    }
}
