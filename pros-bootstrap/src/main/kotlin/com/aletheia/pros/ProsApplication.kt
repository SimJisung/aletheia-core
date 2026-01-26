package com.aletheia.pros

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * PROS (Personal Reasoning OS) Application Entry Point
 *
 * "이 프로젝트는 '결정을 대신하는 AI'가 아니라
 *  '과거의 나를 호출해주는 시스템'을 만드는 일이다."
 *
 * Architecture: Hexagonal (Ports & Adapters)
 * - Domain: Pure business logic (no framework dependencies)
 * - Application: Use cases and port definitions
 * - Infrastructure: Adapters (JPA, LLM, etc.)
 * - API: REST controllers
 */
@SpringBootApplication(
    scanBasePackages = [
        "com.aletheia.pros"
    ]
)
class ProsApplication

fun main(args: Array<String>) {
    runApplication<ProsApplication>(*args)
}
