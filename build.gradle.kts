import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.spring") apply false
    kotlin("plugin.jpa") apply false
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
}

val javaVersion: String by project
val springAiVersion: String by project

allprojects {
    group = property("group") as String
    version = property("version") as String

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt()))
        }
    }

    kotlin {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
            jvmTarget.set(JvmTarget.fromTarget(javaVersion))
        }
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
        }
    }

    dependencies {
        // Kotlin
        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))

        // Logging
        implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

        // Testing
        testImplementation(kotlin("test"))
        testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
        testImplementation("io.mockk:mockk:1.13.14")
        testImplementation("org.assertj:assertj-core:3.27.2")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
