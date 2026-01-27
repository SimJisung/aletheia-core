# CLAUDE.md - AI Assistant Guidelines for aletheia-core

This document provides guidance for AI assistants working on the **aletheia-core** repository, part of the Mnemosyne-Protocol project.

## Project Overview

**aletheia-core** is a personal decision-support system that helps users make decisions based on their own past thoughts and values. The name "Aletheia" (Greek: ἀλήθεια) means "truth" or "disclosure" - reflecting the project's goal of revealing patterns in a user's own thinking rather than providing external recommendations.

### Core Principles

1. **Append-Only Memory**: User thoughts (fragments) are immutable once created; only soft-delete is allowed
2. **Value Graph Contradictions**: The system maps thoughts to value dimensions and visualizes tensions
3. **Separation of Concerns**: Mathematical calculations are deterministic; LLM is only used for explanations
4. **User Agency**: The system shows probabilities, never makes recommendations

### Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Kotlin | 2.3.0 |
| Framework | Spring Boot | 3.5.8 |
| AI Integration | Spring AI | 1.1.2 |
| Database | PostgreSQL | 17 (with pgvector) |
| Build Tool | Gradle | Kotlin DSL |
| JDK | Java | 21 |

## Codebase Structure

```
aletheia-core/
├── pros-bootstrap/           # Spring Boot entry point
├── pros-api/                 # REST API layer (controllers, DTOs)
├── pros-application/         # Application layer (use cases, ports)
├── pros-domain/              # Pure domain models (no dependencies)
├── pros-infrastructure/      # Persistence, AI adapters
├── docs/                     # Project documentation
│   ├── ARCHITECTURE.md       # System architecture details
│   ├── DECISION_PRINCIPLES.md # Design constraints and rules
│   ├── GLOSSARY.md           # Domain terminology
│   └── theoretical-background.md # Academic foundations
├── gradle/                   # Gradle wrapper files
├── build.gradle.kts          # Root build configuration
├── settings.gradle.kts       # Multi-module settings
├── docker-compose.yml        # Development environment
├── .env.example              # Environment template
└── CLAUDE.md                 # This file
```

### Module Architecture (Hexagonal)

The project follows **Hexagonal Architecture** (Clean Architecture) with strict layer separation:

```
┌─────────────────────────────────────────────────────────────┐
│                    pros-bootstrap                            │
│                  (Application Entry)                         │
├─────────────────────────────────────────────────────────────┤
│      pros-api                 │      pros-infrastructure     │
│   (REST Controllers)          │   (JPA, AI Adapters)         │
├─────────────────────────────────────────────────────────────┤
│                    pros-application                          │
│               (Use Cases, Input/Output Ports)                │
├─────────────────────────────────────────────────────────────┤
│                      pros-domain                             │
│           (Pure Models, No Dependencies)                     │
└─────────────────────────────────────────────────────────────┘
```

#### Module Details

| Module | Purpose | Key Contents |
|--------|---------|--------------|
| `pros-domain` | Pure domain models | `ThoughtFragment`, `Decision`, `ValueAxis`, value objects |
| `pros-application` | Business logic | Use cases, input/output port interfaces |
| `pros-infrastructure` | External integrations | JPA entities, repositories, AI adapters, Flyway migrations |
| `pros-api` | HTTP interface | Controllers, request/response DTOs, exception handlers |
| `pros-bootstrap` | Application wiring | `ProsApplication.kt`, configuration, profiles |

### Key Domain Concepts

| Concept | Description |
|---------|-------------|
| `ThoughtFragment` | Immutable user thought with embedding and emotion data |
| `ValueAxis` | 8 fixed dimensions: GROWTH, STABILITY, FINANCIAL, AUTONOMY, RELATIONSHIP, ACHIEVEMENT, HEALTH, MEANING |
| `ValueNode` | Value dimension with aggregated weight from fragments |
| `ValueEdge` | Relationship between value nodes showing tension/alignment |
| `Decision` | Binary choice (A/B) with calculated probabilities |
| `MoodValence` | Emotional polarity (-1.0 to +1.0) |
| `Arousal` | Emotional intensity (0.0 to 1.0) |

## Development Workflow

### Environment Setup

```bash
# 1. Clone and configure
git clone <repository-url>
cd aletheia-core
cp .env.example .env
# Edit .env to add OPENAI_API_KEY

# 2. Start database
docker-compose up -d

# 3. Run application
./gradlew bootRun

# Access API at http://localhost:8080/api
```

### Common Commands

```bash
# Build
./gradlew build                    # Full build with tests
./gradlew bootJar                  # Create executable JAR
./gradlew bootRun                  # Run application

# Testing
./gradlew test                     # Run all tests
./gradlew :pros-domain:test        # Module-specific tests
./gradlew test --info              # Verbose test output

# Database
docker-compose up -d               # Start PostgreSQL
docker-compose down                # Stop containers
docker-compose logs postgres       # View database logs

# Code quality
./gradlew clean build              # Clean rebuild
```

### Git Workflow

1. **Branch Naming Convention**:
   - Feature branches: `feature/<description>`
   - Bug fixes: `fix/<description>`
   - AI-assisted work: `claude/<description>-<session-id>`

2. **Commit Messages**:
   - Use imperative mood: "Add", "Fix", "Update", "Remove"
   - Keep the first line under 72 characters
   - Reference issues when applicable

3. **Pull Requests**:
   - Provide clear descriptions of changes
   - Link related issues
   - Ensure all tests pass before merging

## Key Conventions

### Code Patterns

#### Domain Layer (pros-domain)
```kotlin
// Immutable data classes with factory methods
data class ThoughtFragment private constructor(
    val id: FragmentId,
    val userId: UserId,
    val text: String,
    // ...
) {
    companion object {
        fun create(userId: UserId, text: String, ...): ThoughtFragment
    }
}

// Value objects with inline classes
@JvmInline
value class FragmentId(val value: UUID)

// Validation in constructors
init {
    require(value in -1.0..1.0) { "MoodValence must be between -1.0 and 1.0" }
}
```

#### Application Layer (pros-application)
```kotlin
// Use case with suspend function
class CreateFragmentUseCase(
    private val fragmentRepository: FragmentRepository,
    private val embeddingPort: EmbeddingPort,
) {
    suspend fun execute(command: CreateFragmentCommand): ThoughtFragment
}

// Port interfaces
interface EmbeddingPort {
    suspend fun embed(text: String): Embedding
}
```

#### Infrastructure Layer (pros-infrastructure)
```kotlin
// Repository adapter implementing domain interface
@Repository
class FragmentRepositoryAdapter(
    private val jpaRepository: JpaThoughtFragmentRepository,
    private val mapper: FragmentMapper,
) : FragmentRepository {
    override fun save(fragment: ThoughtFragment): ThoughtFragment
}

// Mapper for domain <-> entity conversion
class FragmentMapper {
    fun toEntity(domain: ThoughtFragment): ThoughtFragmentEntity
    fun toDomain(entity: ThoughtFragmentEntity): ThoughtFragment
}
```

#### API Layer (pros-api)
```kotlin
// Controller with OpenAPI annotations
@RestController
@RequestMapping("/v1/fragments")
@Tag(name = "Fragments")
class FragmentController(private val inputPort: FragmentInputPort) {

    @PostMapping
    @Operation(summary = "Create a new thought fragment")
    fun create(
        @RequestHeader("X-User-Id") userId: UUID,
        @Valid @RequestBody request: CreateFragmentRequest
    ): ResponseEntity<FragmentResponse>
}
```

### Database Conventions

- **Migrations**: Use Flyway in `pros-infrastructure/src/main/resources/db/migration/`
- **Naming**: `V{number}__{description}.sql` (e.g., `V1__create_extensions.sql`)
- **Vector columns**: Use `vector(1536)` type for embeddings
- **Soft delete**: Use `deleted_at` timestamp, never physical delete

### Testing Patterns

```kotlin
// Domain unit tests (pure Kotlin)
@Test
fun `should create fragment with valid data`() {
    val fragment = ThoughtFragment.create(userId, text, embedding, mood, arousal)
    assertThat(fragment.text).isEqualTo(text)
}

// Use case tests with mocks
@Test
fun `should embed and save fragment`() = runTest {
    every { embeddingPort.embed(any()) } returns mockEmbedding
    every { repository.save(any()) } returnsArgument 0

    val result = useCase.execute(command)

    verify { embeddingPort.embed(command.text) }
}

// Integration tests with TestContainers
@SpringBootTest
@Testcontainers
class FragmentRepositoryIntegrationTest {
    @Container
    val postgres = PostgreSQLContainer("postgres:17")
}
```

## For AI Assistants

### Critical Design Constraints

**READ `docs/DECISION_PRINCIPLES.md` BEFORE MAKING CHANGES**

1. **LLM Usage Boundaries**:
   - **FORBIDDEN**: Decision calculation, probability computation, recommendations
   - **ALLOWED**: Text embeddings, emotion analysis, explanation generation

2. **Immutability Rules**:
   - `ThoughtFragment` is append-only (soft-delete only)
   - `Decision` results are immutable once calculated
   - Never modify historical data

3. **Separation of Calculation and Explanation**:
   - Mathematical scoring must be deterministic (no LLM)
   - Explanations are generated after calculations complete
   - Never let explanation logic influence decision scores

### Before Making Changes

1. **Read existing files**: Always read before modifying
2. **Check related modules**: Understand cross-module dependencies
3. **Review domain constraints**: Check `docs/DECISION_PRINCIPLES.md`
4. **Understand the glossary**: Check `docs/GLOSSARY.md` for terminology

### When Implementing Features

1. **Follow hexagonal architecture**: Respect layer boundaries
2. **Start in domain layer**: Define models and interfaces first
3. **Use existing patterns**: Match mapper, adapter, use case patterns
4. **Write tests**: Unit tests for domain, integration tests for infrastructure

### When Modifying Domain Models

1. **Preserve immutability**: Use `data class` with `private constructor`
2. **Add factory methods**: Use companion object `create()` functions
3. **Validate in constructors**: Use `init` blocks with `require()`
4. **Keep pure**: No Spring annotations in `pros-domain`

### Things to Avoid

- Don't add LLM calls to calculation logic
- Don't allow physical deletion of fragments
- Don't add recommendations or advice to decision output
- Don't break hexagonal layer boundaries
- Don't add Spring annotations to domain models
- Don't create mutable domain objects

### Git Operations

- Always verify the current branch before committing
- Use specific file additions rather than `git add -A`
- Never force push to shared branches
- Create new commits rather than amending previous work

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/fragments` | Create thought fragment |
| GET | `/api/v1/fragments` | List fragments (paginated) |
| GET | `/api/v1/fragments/{id}` | Get fragment by ID |
| DELETE | `/api/v1/fragments/{id}` | Soft-delete fragment |
| GET | `/api/v1/fragments/similar` | Semantic similarity search |
| POST | `/api/v1/decisions` | Create decision projection |
| GET | `/api/v1/decisions` | List decisions |
| POST | `/api/v1/decisions/{id}/feedback` | Submit feedback |
| GET | `/api/v1/values` | Get user's value graph |

All endpoints require `X-User-Id` header for user identification.

## Configuration

### Key Properties (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
  jpa:
    hibernate:
      ddl-auto: validate  # Flyway handles migrations
  ai:
    openai:
      embedding:
        options:
          model: text-embedding-3-small
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.3

server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: /api
```

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `DB_HOST` | PostgreSQL host | Yes |
| `DB_PORT` | PostgreSQL port | Yes |
| `DB_NAME` | Database name | Yes |
| `DB_USER` | Database user | Yes |
| `DB_PASSWORD` | Database password | Yes |
| `OPENAI_API_KEY` | OpenAI API key | Yes |
| `SERVER_PORT` | Application port | No (default: 8080) |
| `SPRING_PROFILES_ACTIVE` | Active profile | No (default: dev) |

## Documentation Reference

| Document | Purpose |
|----------|---------|
| `README.md` | Project introduction and quick start |
| `docs/ARCHITECTURE.md` | Detailed system architecture |
| `docs/DECISION_PRINCIPLES.md` | Design constraints and rules (MUST READ) |
| `docs/GLOSSARY.md` | Domain terminology definitions |
| `docs/theoretical-background.md` | Academic foundations |

## Quick Reference

### Eight Value Axes

```kotlin
enum class ValueAxis {
    GROWTH,        // Personal development, learning
    STABILITY,     // Security, predictability
    FINANCIAL,     // Money, economic security
    AUTONOMY,      // Independence, freedom
    RELATIONSHIP,  // Social connections, belonging
    ACHIEVEMENT,   // Success, accomplishment
    HEALTH,        // Physical/mental wellbeing
    MEANING        // Purpose, significance
}
```

### Decision Calculation Formula

```
score = fit - λ × regret
P(A|Me) = exp(scoreA) / (exp(scoreA) + exp(scoreB))
```

Where:
- `fit`: How well option aligns with user's value graph
- `regret`: Estimated regret risk for the option
- `λ (lambda)`: Regret sensitivity parameter (adjustable via feedback)

---

*Last updated: Reflects codebase state as of January 2026*
