# Design: ValueAlignment 사용자 가치 데이터 반영 개선

> **Feature ID**: value-alignment-improvement
> **Plan Reference**: [value-alignment-improvement.plan.md](../../01-plan/features/value-alignment-improvement.plan.md)
> **Version**: 1.0
> **Created**: 2026-02-01
> **Status**: Draft

---

## 1. 설계 개요

### 1.1 목표 요약

사용자가 8개 ValueAxis에 대한 중요도를 명시적으로 설정하고, 이 데이터를 Decision의 valueAlignment 계산에 반영하여 옵션 간 가치 차이를 명확하게 표현합니다.

### 1.2 설계 원칙

| 원칙 | 적용 방식 |
|------|----------|
| **Hexagonal Architecture** | Domain → Application → Infrastructure 순서로 구현 |
| **LLM 금지 영역 준수** | 가치 중요도 계산에 LLM 미사용 |
| **하위 호환성** | 기존 API 스펙 유지, 신규 필드는 선택적 |
| **불변성** | ValueImportance 변경 시 이력 보존 (append-only) |

---

## 2. 도메인 모델 설계

### 2.1 신규 도메인 모델: ValueImportance

**파일 위치**: `pros-domain/src/main/kotlin/com/aletheia/pros/domain/value/ValueImportance.kt`

```kotlin
package com.aletheia.pros.domain.value

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueImportanceId
import java.time.Instant

/**
 * ValueImportance represents a user's explicit importance ratings for value axes.
 *
 * This is separate from ValueNode (which tracks implicit valence from fragments).
 * Users can explicitly set how important each value axis is to them on a 1-10 scale.
 *
 * Design Decision:
 * - Stored as a Map to allow partial updates (not all 8 axes required)
 * - Normalized to 0.0-1.0 internally for calculation consistency
 * - Immutable with version history for audit trail
 */
data class ValueImportance(
    val id: ValueImportanceId,
    val userId: UserId,
    val importanceMap: Map<ValueAxis, Double>,  // Normalized 0.0-1.0
    val version: Int,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    init {
        importanceMap.forEach { (axis, value) ->
            require(value in 0.0..1.0) {
                "Importance for $axis must be between 0.0 and 1.0, got: $value"
            }
        }
        require(version >= 1) { "Version must be at least 1" }
    }

    /**
     * Gets the importance for a specific axis.
     * Returns default value if not explicitly set.
     */
    fun getImportance(axis: ValueAxis): Double {
        return importanceMap[axis] ?: DEFAULT_IMPORTANCE
    }

    /**
     * Whether this axis has an explicit importance value.
     */
    fun hasExplicitImportance(axis: ValueAxis): Boolean {
        return importanceMap.containsKey(axis)
    }

    /**
     * Updates importance values and increments version.
     */
    fun update(
        newImportanceMap: Map<ValueAxis, Double>,
        updatedAt: Instant = Instant.now()
    ): ValueImportance {
        val mergedMap = importanceMap.toMutableMap().apply {
            putAll(newImportanceMap)
        }
        return copy(
            importanceMap = mergedMap,
            version = version + 1,
            updatedAt = updatedAt
        )
    }

    /**
     * Gets all importance values, including defaults for unset axes.
     */
    fun getAllImportances(): Map<ValueAxis, Double> {
        return ValueAxis.all().associateWith { axis ->
            importanceMap[axis] ?: DEFAULT_IMPORTANCE
        }
    }

    companion object {
        /**
         * Default importance when not explicitly set.
         * 0.5 represents neutral importance.
         */
        const val DEFAULT_IMPORTANCE = 0.5

        /**
         * Creates a new ValueImportance with initial values.
         */
        fun create(
            userId: UserId,
            importanceMap: Map<ValueAxis, Double>,
            createdAt: Instant = Instant.now()
        ): ValueImportance {
            // Normalize input (1-10 scale) to 0.0-1.0
            val normalizedMap = importanceMap.mapValues { (_, value) ->
                normalizeFromScale(value)
            }
            return ValueImportance(
                id = ValueImportanceId.generate(),
                userId = userId,
                importanceMap = normalizedMap,
                version = 1,
                createdAt = createdAt,
                updatedAt = createdAt
            )
        }

        /**
         * Creates with all axes set to default.
         */
        fun createDefault(
            userId: UserId,
            createdAt: Instant = Instant.now()
        ): ValueImportance = ValueImportance(
            id = ValueImportanceId.generate(),
            userId = userId,
            importanceMap = emptyMap(),
            version = 1,
            createdAt = createdAt,
            updatedAt = createdAt
        )

        /**
         * Converts 1-10 scale to 0.0-1.0 normalized value.
         */
        fun normalizeFromScale(value: Double, min: Double = 1.0, max: Double = 10.0): Double {
            return ((value - min) / (max - min)).coerceIn(0.0, 1.0)
        }

        /**
         * Converts 0.0-1.0 to 1-10 scale for display.
         */
        fun denormalizeToScale(normalized: Double, min: Double = 1.0, max: Double = 10.0): Double {
            return (normalized * (max - min) + min).coerceIn(min, max)
        }
    }
}
```

### 2.2 신규 ID 타입

**파일 위치**: `pros-domain/src/main/kotlin/com/aletheia/pros/domain/common/Identifiers.kt` (추가)

```kotlin
/**
 * Type-safe identifier for ValueImportance entities.
 */
@JvmInline
value class ValueImportanceId(val value: UUID) {
    companion object {
        fun generate(): ValueImportanceId = ValueImportanceId(UUID.randomUUID())
        fun from(value: String): ValueImportanceId = ValueImportanceId(UUID.fromString(value))
    }

    override fun toString(): String = value.toString()
}
```

### 2.3 Repository 인터페이스

**파일 위치**: `pros-domain/src/main/kotlin/com/aletheia/pros/domain/value/ValueImportanceRepository.kt`

```kotlin
package com.aletheia.pros.domain.value

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueImportanceId

/**
 * Repository interface for ValueImportance persistence.
 */
interface ValueImportanceRepository {

    /**
     * Saves a value importance record.
     * Creates new or updates existing based on ID.
     */
    fun save(importance: ValueImportance): ValueImportance

    /**
     * Finds the current (latest version) importance for a user.
     */
    fun findByUserId(userId: UserId): ValueImportance?

    /**
     * Finds a specific importance record by ID.
     */
    fun findById(id: ValueImportanceId): ValueImportance?

    /**
     * Finds all versions of importance for a user (for audit).
     */
    fun findAllVersionsByUserId(userId: UserId): List<ValueImportance>

    /**
     * Checks if a user has any importance settings.
     */
    fun existsByUserId(userId: UserId): Boolean
}
```

---

## 3. Application Layer 설계

### 3.1 Use Cases

#### 3.1.1 SetValueImportanceUseCase

**파일 위치**: `pros-application/src/main/kotlin/com/aletheia/pros/application/usecase/value/SetValueImportanceUseCase.kt`

```kotlin
package com.aletheia.pros.application.usecase.value

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.value.ValueAxis
import com.aletheia.pros.domain.value.ValueImportance
import com.aletheia.pros.domain.value.ValueImportanceRepository

/**
 * Use case for setting user's value importance ratings.
 */
class SetValueImportanceUseCase(
    private val repository: ValueImportanceRepository
) {

    /**
     * Sets or updates importance ratings.
     *
     * @param command Contains userId and importance values (1-10 scale)
     * @return Updated ValueImportance
     */
    fun execute(command: SetValueImportanceCommand): ValueImportance {
        val existing = repository.findByUserId(command.userId)

        val importance = if (existing != null) {
            // Normalize and update existing
            val normalizedMap = command.importanceValues.mapValues { (_, value) ->
                ValueImportance.normalizeFromScale(value)
            }
            existing.update(normalizedMap)
        } else {
            // Create new
            ValueImportance.create(
                userId = command.userId,
                importanceMap = command.importanceValues
            )
        }

        return repository.save(importance)
    }
}

/**
 * Command for setting value importance.
 */
data class SetValueImportanceCommand(
    val userId: UserId,
    val importanceValues: Map<ValueAxis, Double>  // 1-10 scale
) {
    init {
        importanceValues.forEach { (axis, value) ->
            require(value in 1.0..10.0) {
                "Importance for $axis must be between 1 and 10, got: $value"
            }
        }
    }
}
```

#### 3.1.2 GetValueImportanceUseCase

**파일 위치**: `pros-application/src/main/kotlin/com/aletheia/pros/application/usecase/value/GetValueImportanceUseCase.kt`

```kotlin
package com.aletheia.pros.application.usecase.value

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.value.ValueImportance
import com.aletheia.pros.domain.value.ValueImportanceRepository

/**
 * Use case for retrieving user's value importance ratings.
 */
class GetValueImportanceUseCase(
    private val repository: ValueImportanceRepository
) {

    /**
     * Gets the user's current importance settings.
     * Returns default if not explicitly set.
     */
    fun execute(userId: UserId): ValueImportance {
        return repository.findByUserId(userId)
            ?: ValueImportance.createDefault(userId)
    }
}
```

### 3.2 CreateDecisionUseCase 수정

**파일 위치**: `pros-application/src/main/kotlin/com/aletheia/pros/application/usecase/decision/CreateDecisionUseCase.kt`

**변경 내용**:

```kotlin
class CreateDecisionUseCase(
    private val decisionRepository: DecisionRepository,
    private val fragmentRepository: FragmentRepository,
    private val valueGraphRepository: ValueGraphRepository,
    private val valueImportanceRepository: ValueImportanceRepository,  // 신규 추가
    private val embeddingPort: EmbeddingPort,
    private val userSettingsProvider: UserSettingsProvider
) {
    // ... 기존 코드 ...

    suspend fun execute(command: CreateDecisionCommand): Decision {
        // ... 기존 코드 ...

        // 6.5. Get user's explicit value importance (신규)
        val valueImportance = valueImportanceRepository.findByUserId(command.userId)

        // ... 기존 코드 ...

        val valueAlignment = calculateValueAlignment(
            optionAEmbedding = optionAEmbedding,
            optionBEmbedding = optionBEmbedding,
            nodes = valueGraph?.nodes ?: emptyList(),
            importance = valueImportance  // 신규 파라미터
        )

        // ... 나머지 코드 ...
    }

    /**
     * Calculates how each option aligns with the user's value axes.
     *
     * IMPROVED ALGORITHM:
     * 1. Calculate base similarity difference (existing)
     * 2. Apply user's explicit importance weights (NEW)
     * 3. Apply implicit weights from ValueNode (existing, improved)
     * 4. Normalize with amplification for better differentiation
     */
    private suspend fun calculateValueAlignment(
        optionAEmbedding: Embedding,
        optionBEmbedding: Embedding,
        nodes: List<ValueNode>,
        importance: ValueImportance?  // 신규 파라미터
    ): Map<ValueAxis, Double> {
        val nodeMap = nodes.associateBy { it.axis }
        val alignments = mutableMapOf<ValueAxis, Double>()

        for (axis in ValueAxis.all()) {
            // Step 1: Generate embedding for this value axis
            val axisText = buildAxisText(axis)
            val axisEmbedding = embeddingPort.embed(axisText)

            // Step 2: Calculate similarity of each option to this axis
            val simA = optionAEmbedding.cosineSimilarity(axisEmbedding)
            val simB = optionBEmbedding.cosineSimilarity(axisEmbedding)

            // Step 3: Compute base difference
            val baseDiff = simA - simB  // -1.0 to 1.0

            // Step 4: Apply explicit importance weight (NEW)
            val explicitWeight = importance?.getImportance(axis) ?: DEFAULT_IMPORTANCE
            // Amplify difference for important values (1.0 + importance makes range 1.0-2.0)
            var amplifiedDiff = baseDiff * (1.0 + explicitWeight)

            // Step 5: Apply implicit weight from fragment history (IMPROVED)
            val node = nodeMap[axis]
            if (node != null && node.fragmentCount > 0) {
                // Confidence factor based on fragment count
                val confidence = minOf(node.fragmentCount / CONFIDENCE_THRESHOLD, 1.0)
                // Valence adjustment (positive valence amplifies, negative dampens)
                val valenceAdjustment = 1.0 + (node.avgValence * IMPLICIT_WEIGHT_FACTOR * confidence)
                amplifiedDiff *= valenceAdjustment
            }

            // Step 6: Normalize to 0.0-1.0 range with better spread
            // Max possible diff after amplification is ~4.0 (baseDiff=1.0, explicitWeight=1.0, valence=1.0)
            val normalizedAlignment = (amplifiedDiff / MAX_AMPLIFIED_DIFF + 1.0) / 2.0

            alignments[axis] = normalizedAlignment.coerceIn(0.0, 1.0)
        }

        return alignments
    }

    companion object {
        // ... 기존 상수들 ...

        // 신규 상수
        private const val DEFAULT_IMPORTANCE = 0.5
        private const val IMPLICIT_WEIGHT_FACTOR = 0.5
        private const val CONFIDENCE_THRESHOLD = 10.0
        private const val MAX_AMPLIFIED_DIFF = 4.0
    }
}
```

---

## 4. Infrastructure Layer 설계

### 4.1 Database Migration

**파일 위치**: `pros-infrastructure/src/main/resources/db/migration/V9__create_value_importance.sql`

```sql
-- V9: Create value importance table
-- ================================
-- Stores user's explicit value importance ratings.

CREATE TABLE value_importance (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- User reference
    user_id UUID NOT NULL,

    -- Importance values for each axis (stored as JSONB for flexibility)
    importance_map JSONB NOT NULL DEFAULT '{}',

    -- Version for change tracking
    version INTEGER NOT NULL DEFAULT 1,

    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_version_positive CHECK (version >= 1),
    CONSTRAINT chk_importance_values CHECK (
        -- Validate each value is between 0.0 and 1.0
        NOT EXISTS (
            SELECT 1
            FROM jsonb_each_text(importance_map)
            WHERE value::numeric < 0 OR value::numeric > 1
        )
    )
);

-- Unique constraint: one active record per user
CREATE UNIQUE INDEX idx_value_importance_user ON value_importance(user_id);

-- Index for version history queries
CREATE INDEX idx_value_importance_user_version ON value_importance(user_id, version);

-- Comments
COMMENT ON TABLE value_importance IS 'User explicit value axis importance ratings';
COMMENT ON COLUMN value_importance.importance_map IS 'Map of ValueAxis name to normalized importance (0.0-1.0)';
COMMENT ON COLUMN value_importance.version IS 'Version number, increments on each update';
```

### 4.2 JPA Entity

**파일 위치**: `pros-infrastructure/src/main/kotlin/com/aletheia/pros/infrastructure/persistence/entity/ValueImportanceEntity.kt`

```kotlin
package com.aletheia.pros.infrastructure.persistence.entity

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "value_importance")
class ValueImportanceEntity(
    @Id
    val id: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Type(JsonBinaryType::class)
    @Column(name = "importance_map", columnDefinition = "jsonb", nullable = false)
    val importanceMap: Map<String, Double>,

    @Column(nullable = false)
    val version: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant
)
```

### 4.3 Repository Adapter

**파일 위치**: `pros-infrastructure/src/main/kotlin/com/aletheia/pros/infrastructure/persistence/adapter/ValueImportanceRepositoryAdapter.kt`

```kotlin
package com.aletheia.pros.infrastructure.persistence.adapter

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueImportanceId
import com.aletheia.pros.domain.value.ValueImportance
import com.aletheia.pros.domain.value.ValueImportanceRepository
import com.aletheia.pros.infrastructure.persistence.mapper.ValueImportanceMapper
import com.aletheia.pros.infrastructure.persistence.repository.JpaValueImportanceRepository
import org.springframework.stereotype.Repository

@Repository
class ValueImportanceRepositoryAdapter(
    private val jpaRepository: JpaValueImportanceRepository,
    private val mapper: ValueImportanceMapper
) : ValueImportanceRepository {

    override fun save(importance: ValueImportance): ValueImportance {
        val entity = mapper.toEntity(importance)
        val saved = jpaRepository.save(entity)
        return mapper.toDomain(saved)
    }

    override fun findByUserId(userId: UserId): ValueImportance? {
        return jpaRepository.findByUserId(userId.value)?.let { mapper.toDomain(it) }
    }

    override fun findById(id: ValueImportanceId): ValueImportance? {
        return jpaRepository.findById(id.value).orElse(null)?.let { mapper.toDomain(it) }
    }

    override fun findAllVersionsByUserId(userId: UserId): List<ValueImportance> {
        return jpaRepository.findAllByUserIdOrderByVersionDesc(userId.value)
            .map { mapper.toDomain(it) }
    }

    override fun existsByUserId(userId: UserId): Boolean {
        return jpaRepository.existsByUserId(userId.value)
    }
}
```

### 4.4 JPA Repository

**파일 위치**: `pros-infrastructure/src/main/kotlin/com/aletheia/pros/infrastructure/persistence/repository/JpaValueImportanceRepository.kt`

```kotlin
package com.aletheia.pros.infrastructure.persistence.repository

import com.aletheia.pros.infrastructure.persistence.entity.ValueImportanceEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface JpaValueImportanceRepository : JpaRepository<ValueImportanceEntity, UUID> {

    fun findByUserId(userId: UUID): ValueImportanceEntity?

    fun findAllByUserIdOrderByVersionDesc(userId: UUID): List<ValueImportanceEntity>

    fun existsByUserId(userId: UUID): Boolean
}
```

### 4.5 Mapper

**파일 위치**: `pros-infrastructure/src/main/kotlin/com/aletheia/pros/infrastructure/persistence/mapper/ValueImportanceMapper.kt`

```kotlin
package com.aletheia.pros.infrastructure.persistence.mapper

import com.aletheia.pros.domain.common.UserId
import com.aletheia.pros.domain.common.ValueImportanceId
import com.aletheia.pros.domain.value.ValueAxis
import com.aletheia.pros.domain.value.ValueImportance
import com.aletheia.pros.infrastructure.persistence.entity.ValueImportanceEntity
import org.springframework.stereotype.Component

@Component
class ValueImportanceMapper {

    fun toEntity(domain: ValueImportance): ValueImportanceEntity {
        return ValueImportanceEntity(
            id = domain.id.value,
            userId = domain.userId.value,
            importanceMap = domain.importanceMap.mapKeys { it.key.name },
            version = domain.version,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }

    fun toDomain(entity: ValueImportanceEntity): ValueImportance {
        val importanceMap = entity.importanceMap.mapNotNull { (key, value) ->
            ValueAxis.fromName(key)?.let { axis -> axis to value }
        }.toMap()

        return ValueImportance(
            id = ValueImportanceId(entity.id),
            userId = UserId(entity.userId),
            importanceMap = importanceMap,
            version = entity.version,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
```

---

## 5. API Layer 설계

### 5.1 Controller

**파일 위치**: `pros-api/src/main/kotlin/com/aletheia/pros/api/controller/ValueImportanceController.kt`

```kotlin
package com.aletheia.pros.api.controller

import com.aletheia.pros.api.dto.request.SetValueImportanceRequest
import com.aletheia.pros.api.dto.response.ValueImportanceResponse
import com.aletheia.pros.api.security.SecurityUtils
import com.aletheia.pros.application.usecase.value.GetValueImportanceUseCase
import com.aletheia.pros.application.usecase.value.SetValueImportanceCommand
import com.aletheia.pros.application.usecase.value.SetValueImportanceUseCase
import com.aletheia.pros.domain.value.ValueAxis
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/values/importance")
@Tag(name = "Value Importance", description = "User value importance management")
class ValueImportanceController(
    private val setValueImportanceUseCase: SetValueImportanceUseCase,
    private val getValueImportanceUseCase: GetValueImportanceUseCase
) {

    /**
     * Gets the current user's value importance settings.
     */
    @GetMapping
    @Operation(summary = "Get current user's value importance ratings")
    fun getImportance(): ResponseEntity<ValueImportanceResponse> {
        val userId = SecurityUtils.getCurrentUserId()
        val importance = getValueImportanceUseCase.execute(userId)
        return ResponseEntity.ok(ValueImportanceResponse.from(importance))
    }

    /**
     * Sets or updates value importance ratings.
     */
    @PutMapping
    @Operation(summary = "Set value importance ratings")
    fun setImportance(
        @Valid @RequestBody request: SetValueImportanceRequest
    ): ResponseEntity<ValueImportanceResponse> {
        val userId = SecurityUtils.getCurrentUserId()

        val importanceMap = request.importance.mapNotNull { (key, value) ->
            ValueAxis.fromName(key)?.let { axis -> axis to value }
        }.toMap()

        val command = SetValueImportanceCommand(
            userId = userId,
            importanceValues = importanceMap
        )

        val result = setValueImportanceUseCase.execute(command)
        return ResponseEntity.ok(ValueImportanceResponse.from(result))
    }
}
```

### 5.2 Request DTO

**파일 위치**: `pros-api/src/main/kotlin/com/aletheia/pros/api/dto/request/ValueImportanceRequests.kt`

```kotlin
package com.aletheia.pros.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

/**
 * Request for setting value importance ratings.
 */
@Schema(description = "Request to set value importance ratings")
data class SetValueImportanceRequest(
    @Schema(
        description = "Map of value axis name to importance score (1-10)",
        example = """{"GROWTH": 9, "STABILITY": 4, "AUTONOMY": 8}"""
    )
    @field:Size(min = 1, max = 8, message = "Must provide 1-8 importance values")
    val importance: Map<String, @Min(1) @Max(10) Double>
)
```

### 5.3 Response DTO

**파일 위치**: `pros-api/src/main/kotlin/com/aletheia/pros/api/dto/response/ValueImportanceResponse.kt`

```kotlin
package com.aletheia.pros.api.dto.response

import com.aletheia.pros.domain.value.ValueImportance
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/**
 * Response containing value importance ratings.
 */
@Schema(description = "User's value importance ratings")
data class ValueImportanceResponse(
    @Schema(description = "Map of value axis to importance (1-10 scale)")
    val importance: Map<String, Double>,

    @Schema(description = "Version number of this configuration")
    val version: Int,

    @Schema(description = "Last update timestamp")
    val updatedAt: Instant
) {
    companion object {
        fun from(domain: ValueImportance): ValueImportanceResponse {
            // Convert all importances to 1-10 scale for display
            val displayImportance = domain.getAllImportances()
                .mapKeys { it.key.name }
                .mapValues { ValueImportance.denormalizeToScale(it.value) }

            return ValueImportanceResponse(
                importance = displayImportance,
                version = domain.version,
                updatedAt = domain.updatedAt
            )
        }
    }
}
```

---

## 6. API 스펙

### 6.1 GET /v1/values/importance

**설명**: 현재 사용자의 가치 중요도 설정 조회

**응답 예시**:
```json
{
  "importance": {
    "GROWTH": 9.0,
    "STABILITY": 4.0,
    "FINANCIAL": 7.0,
    "AUTONOMY": 8.0,
    "RELATIONSHIP": 6.0,
    "ACHIEVEMENT": 5.5,
    "HEALTH": 7.0,
    "MEANING": 9.0
  },
  "version": 3,
  "updatedAt": "2026-02-01T10:30:00Z"
}
```

### 6.2 PUT /v1/values/importance

**설명**: 가치 중요도 설정/업데이트

**요청 예시**:
```json
{
  "importance": {
    "GROWTH": 9,
    "STABILITY": 4,
    "AUTONOMY": 8
  }
}
```

**응답**: GET과 동일한 형식

---

## 7. 계산 알고리즘 상세

### 7.1 개선된 valueAlignment 계산 흐름

```
┌─────────────────────────────────────────────────────────────────────┐
│                    calculateValueAlignment()                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  FOR each axis in ValueAxis.all():                                  │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ Step 1: 기본 유사도 계산                                        │ │
│  │   simA = optionA.cosineSimilarity(axisEmbedding)               │ │
│  │   simB = optionB.cosineSimilarity(axisEmbedding)               │ │
│  │   baseDiff = simA - simB  // Range: [-1.0, 1.0]                │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                            ↓                                        │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ Step 2: 명시적 중요도 적용 (NEW)                                │ │
│  │   explicitWeight = importance?.getImportance(axis) ?: 0.5      │ │
│  │   amplifiedDiff = baseDiff * (1.0 + explicitWeight)            │ │
│  │   // Range: [-2.0, 2.0] when explicitWeight=1.0                │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                            ↓                                        │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ Step 3: 암묵적 가중치 적용 (IMPROVED)                           │ │
│  │   IF node.fragmentCount > 0:                                   │ │
│  │     confidence = min(fragmentCount / 10.0, 1.0)                │ │
│  │     valenceAdj = 1.0 + (avgValence * 0.5 * confidence)         │ │
│  │     amplifiedDiff *= valenceAdj                                │ │
│  │   // Range: [-4.0, 4.0] max                                    │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                            ↓                                        │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ Step 4: 정규화                                                  │ │
│  │   alignment = (amplifiedDiff / 4.0 + 1.0) / 2.0                │ │
│  │   alignment = alignment.coerceIn(0.0, 1.0)                     │ │
│  │   // Final range: [0.0, 1.0]                                   │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  RETURN alignments                                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 7.2 예상 결과 비교

**현재 (모든 값 ~0.5 주변)**:
```json
{
  "GROWTH": 0.5448,
  "STABILITY": 0.5014,
  "FINANCIAL": 0.4818,
  "AUTONOMY": 0.5304,
  "RELATIONSHIP": 0.4819,
  "ACHIEVEMENT": 0.4944,
  "HEALTH": 0.4718,
  "MEANING": 0.5203
}
```

**개선 후 (사용자가 GROWTH=9, STABILITY=3 설정 시)**:
```json
{
  "GROWTH": 0.7820,      // 중요도 높음 → 차이 확대
  "STABILITY": 0.4506,   // 중요도 낮음 → 차이 축소
  "FINANCIAL": 0.4627,
  "AUTONOMY": 0.6508,
  "RELATIONSHIP": 0.4312,
  "ACHIEVEMENT": 0.4889,
  "HEALTH": 0.4436,
  "MEANING": 0.7102
}
```

---

## 8. 구현 순서

### Phase 1: Domain Layer
1. `ValueImportanceId` 추가 (Identifiers.kt)
2. `ValueImportance` 도메인 모델 생성
3. `ValueImportanceRepository` 인터페이스 정의

### Phase 2: Application Layer
4. `SetValueImportanceUseCase` 구현
5. `GetValueImportanceUseCase` 구현
6. `CreateDecisionUseCase` 수정

### Phase 3: Infrastructure Layer
7. `V9__create_value_importance.sql` 마이그레이션
8. `ValueImportanceEntity` JPA 엔티티
9. `JpaValueImportanceRepository` 인터페이스
10. `ValueImportanceMapper` 구현
11. `ValueImportanceRepositoryAdapter` 구현
12. `UseCaseConfig`에 빈 등록

### Phase 4: API Layer
13. Request/Response DTO 생성
14. `ValueImportanceController` 구현

### Phase 5: Testing
15. 도메인 단위 테스트
16. UseCase 테스트
17. Repository 통합 테스트
18. API 통합 테스트

---

## 9. 테스트 계획

### 9.1 단위 테스트

| 테스트 대상 | 테스트 케이스 |
|------------|--------------|
| ValueImportance | 생성, 업데이트, 정규화 검증 |
| SetValueImportanceUseCase | 신규 생성, 기존 업데이트 |
| calculateValueAlignment | 중요도 적용 전/후 차이 검증 |

### 9.2 통합 테스트

| 테스트 대상 | 테스트 케이스 |
|------------|--------------|
| ValueImportanceRepository | CRUD 작업, 버전 이력 조회 |
| API Endpoints | GET/PUT 요청 처리, 인증 검증 |

### 9.3 검증 기준 (Plan 문서 참조)

- [ ] FR-1: 8개 ValueAxis별 중요도 설정 가능
- [ ] FR-2: 중요도가 valueAlignment 계산에 반영
- [ ] FR-3: 중요도 미설정 시 기존 로직 동작 (하위호환)
- [ ] FR-4: 중요도 업데이트 가능
- [ ] NFR-2: 값 분산도(표준편차) > 0.15

---

## 10. 의존성

### 10.1 신규 의존성
- 없음 (기존 프레임워크 활용)

### 10.2 영향받는 기존 컴포넌트

| 컴포넌트 | 변경 유형 |
|---------|----------|
| CreateDecisionUseCase | 수정 (신규 파라미터 추가) |
| UseCaseConfig | 수정 (신규 빈 등록) |
| Identifiers.kt | 수정 (ValueImportanceId 추가) |

---

## 11. 승인

| 역할 | 이름 | 승인일 |
|------|------|-------|
| Tech Lead | - | - |
| Backend Developer | - | - |

---

*Generated by PDCA Process*
