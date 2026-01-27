/**
 * pros-application: Application Layer (Use Cases)
 *
 * This module contains:
 * - Use case implementations
 * - Application services
 * - Port interfaces for infrastructure
 * - DTOs for cross-layer communication
 *
 * Dependencies: pros-domain only
 */

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":pros-domain"))

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
