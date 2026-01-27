/**
 * pros-infrastructure: Infrastructure Layer
 *
 * This module contains:
 * - JPA entities and repositories
 * - Spring AI / MCP integration
 * - Embedding service adapters
 * - Database configurations
 *
 * Dependencies: pros-domain, pros-application, Spring frameworks
 */

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":pros-domain"))
    implementation(project(":pros-application"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Spring AI - Core
    implementation("org.springframework.ai:spring-ai-core")

    // Spring AI - OpenAI (for embeddings and LLM)
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")

    // Spring AI - PGVector Store
    implementation("org.springframework.ai:spring-ai-pgvector-store-spring-boot-starter")

    // Spring AI - MCP (Model Context Protocol)
    implementation("org.springframework.ai:spring-ai-mcp")

    // PostgreSQL
    runtimeOnly("org.postgresql:postgresql")

    // pgvector Java support
    implementation("com.pgvector:pgvector:0.1.6")

    // Database migration
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Jackson for JSON
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql:2.0.3")
    testImplementation("org.testcontainers:junit-jupiter:2.0.3")
}
