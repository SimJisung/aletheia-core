/**
 * pros-domain: Pure Kotlin Domain Models
 *
 * This module contains:
 * - Domain entities (ThoughtFragment, ValueNode, ValueEdge, Decision)
 * - Value objects (FragmentId, UserId, ValueAxis, etc.)
 * - Domain services (pure business logic)
 * - Repository interfaces (ports)
 *
 * Dependencies: NONE (pure Kotlin only)
 * This ensures domain logic is framework-agnostic and highly testable.
 */

plugins {
    kotlin("jvm")
}

dependencies {
    // No external dependencies - pure Kotlin domain
    // Only standard library and kotlin-reflect are included from parent
}
