# Design: Decision Explainability 개선

> Decision API 응답에 계산 근거를 포함하여 투명성 확보

**Plan 문서**: `docs/01-plan/features/decision-explainability.plan.md`

---

## 1. 아키텍처 설계

### 1.1 계산 흐름 (기존 vs 개선)

```
기존 흐름:
CreateDecisionUseCase.execute()
    ├─ calculateValueFit() ──→ (fitA, fitB) [중간값 폐기]
    ├─ calculateRegretRisk() ──→ (regretA, regretB) [중간값 폐기]
    └─ DecisionResult.compute() ──→ 최종 확률만 저장

개선 흐름:
CreateDecisionUseCase.execute()
    ├─ calculateValueFit() ──→ FitBreakdown [중간값 보존]
    │      └─ fragmentContributions[]
    ├─ calculateRegretRisk() ──→ RegretBreakdown [중간값 보존]
    │      └─ historicalRate, variance, negativity
    └─ DecisionResult.compute() ──→ DecisionResult + CalculationBreakdown
           └─ 모든 중간값 포함
```

### 1.2 패키지 구조

```
pros-domain/src/main/kotlin/com/aletheia/pros/domain/decision/
├── Decision.kt                    # 기존
├── DecisionResult.kt              # 기존 분리
├── breakdown/                     # NEW: 계산 상세 값객체들
│   ├── CalculationBreakdown.kt    # 집합 객체
│   ├── FitBreakdown.kt            # 적합도 상세
│   ├── RegretBreakdown.kt         # 후회 위험 상세
│   └── FragmentContribution.kt    # Fragment별 기여도

pros-application/src/main/kotlin/com/aletheia/pros/application/usecase/decision/
├── CreateDecisionUseCase.kt       # MODIFY: breakdown 생성 로직

pros-api/src/main/kotlin/com/aletheia/pros/api/dto/response/
├── DecisionResponses.kt           # MODIFY: breakdown 응답 추가
└── CalculationBreakdownResponse.kt # NEW: breakdown DTO
```

---

## 2. 도메인 모델 설계

### 2.1 CalculationBreakdown (집합 객체)

**파일**: `pros-domain/src/main/kotlin/com/aletheia/pros/domain/decision/breakdown/CalculationBreakdown.kt`

```kotlin
package com.aletheia.pros.domain.decision.breakdown

/**
 * 결정 계산의 전체 분해 상세.
 * 모든 중간 계산값을 보존하여 설명 가능성을 확보한다.
 *
 * DESIGN PRINCIPLE:
 * - 순수 계산값만 포함 (LLM 생성 텍스트 없음)
 * - 불변 객체
 * - 모든 값은 재현 가능
 */
data class CalculationBreakdown(
    val fit: FitBreakdown,
    val regret: RegretBreakdown,
    val parameters: CalculationParameters,
    val scores: ScoreBreakdown
) {
    companion object {
        fun compute(
            fit: FitBreakdown,
            regret: RegretBreakdown,
            parameters: CalculationParameters
        ): CalculationBreakdown {
            val scoreA = fit.fitScoreA - parameters.lambda * regret.regretRiskA
            val scoreB = fit.fitScoreB - parameters.lambda * regret.regretRiskB

            return CalculationBreakdown(
                fit = fit,
                regret = regret,
                parameters = parameters,
                scores = ScoreBreakdown(
                    scoreA = scoreA,
                    scoreB = scoreB,
                    formula = "score = fit - lambda × regret"
                )
            )
        }
    }
}

/**
 * 최종 점수 계산 상세.
 */
data class ScoreBreakdown(
    val scoreA: Double,
    val scoreB: Double,
    val formula: String
)

/**
 * 계산에 사용된 파라미터들.
 */
data class CalculationParameters(
    val lambda: Double,
    val regretPrior: Double,
    val priorityAxisBoost: Double,
    val volatilityWeight: Double,
    val negativityWeight: Double
) {
    companion object {
        fun default(lambda: Double, regretPrior: Double) = CalculationParameters(
            lambda = lambda,
            regretPrior = regretPrior,
            priorityAxisBoost = 0.35,
            volatilityWeight = 0.3,
            negativityWeight = 0.3
        )
    }
}
```

---

### 2.2 FitBreakdown (적합도 상세)

**파일**: `pros-domain/src/main/kotlin/com/aletheia/pros/domain/decision/breakdown/FitBreakdown.kt`

```kotlin
package com.aletheia.pros.domain.decision.breakdown

import com.aletheia.pros.domain.common.FragmentId

/**
 * 패턴 적합도 계산의 분해 상세.
 *
 * 각 옵션이 사용자의 과거 패턴(fragment)과 얼마나 유사한지 계산한 결과.
 */
data class FitBreakdown(
    /** 옵션 A의 적합도 점수 (0.0~1.0) */
    val fitScoreA: Double,

    /** 옵션 B의 적합도 점수 (0.0~1.0) */
    val fitScoreB: Double,

    /** 계산에 사용된 총 가중치 */
    val totalWeight: Double,

    /** 우선축 부스트 계수 (기본 0.35) */
    val priorityAxisBoost: Double,

    /** 각 fragment의 기여도 목록 */
    val fragmentContributions: List<FragmentContribution>
) {
    init {
        require(fitScoreA in 0.0..1.0) { "fitScoreA must be in [0.0, 1.0]" }
        require(fitScoreB in 0.0..1.0) { "fitScoreB must be in [0.0, 1.0]" }
    }

    /** A가 더 적합한지 여부 */
    val isOptionAMoreFit: Boolean get() = fitScoreA > fitScoreB

    /** 적합도 차이 (양수면 A가 더 적합) */
    val fitDifference: Double get() = fitScoreA - fitScoreB
}

/**
 * 개별 fragment가 결정에 미친 기여도.
 */
data class FragmentContribution(
    /** Fragment ID */
    val fragmentId: FragmentId,

    /** Fragment 텍스트 요약 (최대 80자) */
    val fragmentSummary: String,

    /** 결정 컨텍스트와의 의미적 유사도 (0.0~1.0) */
    val similarity: Double,

    /** Fragment의 감정 가중치 (0.0~1.0, 긍정일수록 높음) */
    val valenceWeight: Double,

    /** 우선축 가중치 (기본 1.0, 우선축과 관련시 증가) */
    val priorityWeight: Double,

    /** 옵션 A에 대한 기여도 */
    val contributionToA: Double,

    /** 옵션 B에 대한 기여도 */
    val contributionToB: Double
) {
    init {
        require(fragmentSummary.length <= MAX_SUMMARY_LENGTH) {
            "fragmentSummary exceeds $MAX_SUMMARY_LENGTH characters"
        }
    }

    /** 총 기여도 (A + B) */
    val totalContribution: Double get() = contributionToA + contributionToB

    /** A쪽으로 더 기여하는지 */
    val favorsOptionA: Boolean get() = contributionToA > contributionToB

    companion object {
        const val MAX_SUMMARY_LENGTH = 80
    }
}
```

---

### 2.3 RegretBreakdown (후회 위험 상세)

**파일**: `pros-domain/src/main/kotlin/com/aletheia/pros/domain/decision/breakdown/RegretBreakdown.kt`

```kotlin
package com.aletheia.pros.domain.decision.breakdown

/**
 * 후회 위험 계산의 분해 상세.
 *
 * 후회 위험은 다음 요소들의 조합으로 계산됨:
 * 1. 과거 피드백 기반 후회율
 * 2. 유사 fragment의 감정 분산 (불확실성)
 * 3. 각 옵션의 부정적 fragment 유사도
 */
data class RegretBreakdown(
    /** 과거 피드백 기반 후회율 (0.0~1.0) */
    val historicalRegretRate: Double,

    /** 유사 fragment들의 감정 분산 (0.0~1.0) - 불확실성 지표 */
    val valenceVariance: Double,

    /** 옵션 A와 부정적 fragment의 유사도 (0.0~1.0) */
    val optionNegativityA: Double,

    /** 옵션 B와 부정적 fragment의 유사도 (0.0~1.0) */
    val optionNegativityB: Double,

    /** 기본 후회값 = historicalRate + (variance × 0.3) */
    val baseRegret: Double,

    /** 최종 후회 위험 A */
    val regretRiskA: Double,

    /** 최종 후회 위험 B */
    val regretRiskB: Double,

    /** 피드백 샘플 수 */
    val feedbackCount: Int,

    /** 계산 공식 */
    val formula: String = "baseRegret + (negativity - 0.5) × 0.3"
) {
    init {
        require(historicalRegretRate in 0.0..1.0) { "historicalRegretRate must be in [0.0, 1.0]" }
        require(valenceVariance in 0.0..1.0) { "valenceVariance must be in [0.0, 1.0]" }
        require(regretRiskA in 0.0..1.0) { "regretRiskA must be in [0.0, 1.0]" }
        require(regretRiskB in 0.0..1.0) { "regretRiskB must be in [0.0, 1.0]" }
    }

    /** 데이터 신뢰도 (피드백 수 기반) */
    val dataReliability: DataReliability
        get() = when {
            feedbackCount >= 10 -> DataReliability.HIGH
            feedbackCount >= 3 -> DataReliability.MEDIUM
            else -> DataReliability.LOW
        }

    /** A가 더 안전한 선택인지 */
    val isOptionASafer: Boolean get() = regretRiskA < regretRiskB
}

/**
 * 데이터 신뢰도 수준.
 */
enum class DataReliability {
    HIGH,   // 10개 이상 피드백
    MEDIUM, // 3~9개 피드백
    LOW     // 3개 미만 피드백 (기본값 사용)
}
```

---

### 2.4 DecisionResult 수정

**파일**: `pros-domain/src/main/kotlin/com/aletheia/pros/domain/decision/Decision.kt`

```kotlin
// 기존 DecisionResult에 breakdown 필드 추가

data class DecisionResult(
    val probabilityA: Probability,
    val probabilityB: Probability,
    val regretRiskA: RegretRisk,
    val regretRiskB: RegretRisk,
    val evidenceFragmentIds: List<FragmentId>,
    val valueAlignment: Map<ValueAxis, Double>,

    // NEW: 계산 상세 (nullable - 하위호환)
    val breakdown: CalculationBreakdown? = null
) {
    // ... 기존 코드 ...

    companion object {
        /**
         * breakdown 없이 계산 (기존 방식, 하위호환)
         */
        fun compute(
            fitA: Double,
            fitB: Double,
            regretA: Double,
            regretB: Double,
            lambda: Double,
            evidenceIds: List<FragmentId>,
            valueAlignment: Map<ValueAxis, Double>
        ): DecisionResult {
            return computeWithBreakdown(
                fitA, fitB, regretA, regretB, lambda, evidenceIds, valueAlignment,
                breakdown = null
            )
        }

        /**
         * breakdown 포함 계산 (새 방식)
         */
        fun computeWithBreakdown(
            fitA: Double,
            fitB: Double,
            regretA: Double,
            regretB: Double,
            lambda: Double,
            evidenceIds: List<FragmentId>,
            valueAlignment: Map<ValueAxis, Double>,
            breakdown: CalculationBreakdown?
        ): DecisionResult {
            val scoreA = fitA - lambda * regretA
            val scoreB = fitB - lambda * regretB

            val expA = kotlin.math.exp(scoreA)
            val expB = kotlin.math.exp(scoreB)
            val sumExp = expA + expB

            val probA = expA / sumExp
            val probB = expB / sumExp

            return DecisionResult(
                probabilityA = Probability(probA),
                probabilityB = Probability(probB),
                regretRiskA = RegretRisk(regretA.coerceIn(0.0, 1.0)),
                regretRiskB = RegretRisk(regretB.coerceIn(0.0, 1.0)),
                evidenceFragmentIds = evidenceIds.take(MAX_EVIDENCE_COUNT),
                valueAlignment = valueAlignment,
                breakdown = breakdown
            )
        }
    }
}
```

---

## 3. UseCase 수정

### 3.1 CreateDecisionUseCase 변경점

**파일**: `pros-application/src/main/kotlin/com/aletheia/pros/application/usecase/decision/CreateDecisionUseCase.kt`

```kotlin
class CreateDecisionUseCase(
    // ... 기존 의존성 ...
) {
    suspend fun execute(command: CreateDecisionCommand): Decision {
        // ... 기존 1~5단계 ...

        // 6. Calculate value fit WITH breakdown
        val fitBreakdown = calculateValueFitWithBreakdown(
            optionAEmbedding = optionAEmbedding,
            optionBEmbedding = optionBEmbedding,
            similarFragments = similarFragments,
            priorityAxis = command.priorityAxis
        )

        // 7. Calculate regret risk WITH breakdown
        val feedbackStats = decisionRepository.getFeedbackStats(command.userId)
        val regretBreakdown = calculateRegretRiskWithBreakdown(
            similarFragments = similarFragments,
            basePrior = userSettings.regretPrior,
            optionAEmbedding = optionAEmbedding,
            optionBEmbedding = optionBEmbedding,
            feedbackStats = feedbackStats
        )

        // 8. Build calculation parameters
        val parameters = CalculationParameters.default(
            lambda = userSettings.lambda,
            regretPrior = userSettings.regretPrior
        )

        // 9. Create calculation breakdown
        val calculationBreakdown = CalculationBreakdown.compute(
            fit = fitBreakdown,
            regret = regretBreakdown,
            parameters = parameters
        )

        // 10. Compute decision result with breakdown
        val result = DecisionResult.computeWithBreakdown(
            fitA = fitBreakdown.fitScoreA,
            fitB = fitBreakdown.fitScoreB,
            regretA = regretBreakdown.regretRiskA,
            regretB = regretBreakdown.regretRiskB,
            lambda = userSettings.lambda,
            evidenceIds = evidenceIds,
            valueAlignment = valueAlignment,
            breakdown = calculationBreakdown
        )

        // ... 기존 11~12단계 ...
    }

    /**
     * 적합도 계산 + breakdown 생성
     */
    private suspend fun calculateValueFitWithBreakdown(
        optionAEmbedding: Embedding,
        optionBEmbedding: Embedding,
        similarFragments: List<SimilarFragment>,
        priorityAxis: ValueAxis?
    ): FitBreakdown {
        if (similarFragments.isEmpty()) {
            return FitBreakdown(
                fitScoreA = 0.5,
                fitScoreB = 0.5,
                totalWeight = 0.0,
                priorityAxisBoost = PRIORITY_AXIS_BOOST,
                fragmentContributions = emptyList()
            )
        }

        val priorityEmbedding = priorityAxis?.let { axis ->
            embeddingPort.embed(buildPriorityAxisText(axis))
        }

        var fitA = 0.0
        var fitB = 0.0
        var totalWeight = 0.0
        val contributions = mutableListOf<FragmentContribution>()

        for (similar in similarFragments) {
            val fragmentEmbedding = similar.fragment.embedding ?: continue

            // Priority weight calculation
            val priorityWeight = priorityEmbedding?.let { axisEmbedding ->
                val axisSimilarity = fragmentEmbedding.cosineSimilarity(axisEmbedding)
                val axisRelevance = axisSimilarity.coerceAtLeast(0.0)
                1.0 + (PRIORITY_AXIS_BOOST * axisRelevance)
            } ?: 1.0

            val weight = similar.similarity * priorityWeight

            val alignA = optionAEmbedding.cosineSimilarity(fragmentEmbedding)
            val alignB = optionBEmbedding.cosineSimilarity(fragmentEmbedding)
            val valenceWeight = (1 + similar.fragment.moodValence.value) / 2

            val contribA = alignA * weight * valenceWeight
            val contribB = alignB * weight * valenceWeight

            fitA += contribA
            fitB += contribB
            totalWeight += weight

            // Capture contribution
            contributions.add(
                FragmentContribution(
                    fragmentId = similar.fragment.id,
                    fragmentSummary = similar.fragment.text.take(FragmentContribution.MAX_SUMMARY_LENGTH),
                    similarity = similar.similarity,
                    valenceWeight = valenceWeight,
                    priorityWeight = priorityWeight,
                    contributionToA = contribA,
                    contributionToB = contribB
                )
            )
        }

        if (totalWeight > 0) {
            fitA /= totalWeight
            fitB /= totalWeight
        }

        return FitBreakdown(
            fitScoreA = fitA.coerceIn(0.0, 1.0),
            fitScoreB = fitB.coerceIn(0.0, 1.0),
            totalWeight = totalWeight,
            priorityAxisBoost = PRIORITY_AXIS_BOOST,
            fragmentContributions = contributions.sortedByDescending { it.totalContribution }.take(10)
        )
    }

    /**
     * 후회 위험 계산 + breakdown 생성
     */
    private fun calculateRegretRiskWithBreakdown(
        similarFragments: List<SimilarFragment>,
        basePrior: Double,
        optionAEmbedding: Embedding,
        optionBEmbedding: Embedding,
        feedbackStats: FeedbackStats
    ): RegretBreakdown {
        val historicalRegretRate = if (feedbackStats.totalWithFeedback > 0) {
            feedbackStats.regretRate
        } else {
            basePrior
        }

        val valenceVariance = calculateValenceVariance(similarFragments)
        val optionNegativityA = calculateOptionNegativity(optionAEmbedding, similarFragments)
        val optionNegativityB = calculateOptionNegativity(optionBEmbedding, similarFragments)

        val baseRegret = historicalRegretRate + valenceVariance * 0.3
        val regretA = (baseRegret + (optionNegativityA - 0.5) * 0.3).coerceIn(0.0, 1.0)
        val regretB = (baseRegret + (optionNegativityB - 0.5) * 0.3).coerceIn(0.0, 1.0)

        return RegretBreakdown(
            historicalRegretRate = historicalRegretRate,
            valenceVariance = valenceVariance,
            optionNegativityA = optionNegativityA,
            optionNegativityB = optionNegativityB,
            baseRegret = baseRegret,
            regretRiskA = regretA,
            regretRiskB = regretB,
            feedbackCount = feedbackStats.totalWithFeedback
        )
    }
}
```

---

## 4. API 응답 설계

### 4.1 CalculationBreakdownResponse DTO

**파일**: `pros-api/src/main/kotlin/com/aletheia/pros/api/dto/response/CalculationBreakdownResponse.kt`

```kotlin
package com.aletheia.pros.api.dto.response

import com.aletheia.pros.domain.decision.breakdown.*

/**
 * 계산 상세 응답 DTO.
 * ?detail=true 파라미터로 요청시에만 포함됨.
 */
data class CalculationBreakdownResponse(
    val fit: FitBreakdownResponse,
    val regret: RegretBreakdownResponse,
    val parameters: CalculationParametersResponse,
    val scores: ScoreBreakdownResponse
) {
    companion object {
        fun from(breakdown: CalculationBreakdown): CalculationBreakdownResponse {
            return CalculationBreakdownResponse(
                fit = FitBreakdownResponse.from(breakdown.fit),
                regret = RegretBreakdownResponse.from(breakdown.regret),
                parameters = CalculationParametersResponse.from(breakdown.parameters),
                scores = ScoreBreakdownResponse.from(breakdown.scores)
            )
        }
    }
}

data class FitBreakdownResponse(
    val fitScoreA: Double,
    val fitScoreB: Double,
    val totalWeight: Double,
    val priorityAxisBoost: Double,
    val fragmentContributions: List<FragmentContributionResponse>
) {
    companion object {
        fun from(fit: FitBreakdown): FitBreakdownResponse {
            return FitBreakdownResponse(
                fitScoreA = fit.fitScoreA,
                fitScoreB = fit.fitScoreB,
                totalWeight = fit.totalWeight,
                priorityAxisBoost = fit.priorityAxisBoost,
                fragmentContributions = fit.fragmentContributions.map {
                    FragmentContributionResponse.from(it)
                }
            )
        }
    }
}

data class FragmentContributionResponse(
    val fragmentId: String,
    val fragmentSummary: String,
    val similarity: Double,
    val valenceWeight: Double,
    val priorityWeight: Double,
    val contributionToA: Double,
    val contributionToB: Double
) {
    companion object {
        fun from(contrib: FragmentContribution): FragmentContributionResponse {
            return FragmentContributionResponse(
                fragmentId = contrib.fragmentId.toString(),
                fragmentSummary = contrib.fragmentSummary,
                similarity = contrib.similarity,
                valenceWeight = contrib.valenceWeight,
                priorityWeight = contrib.priorityWeight,
                contributionToA = contrib.contributionToA,
                contributionToB = contrib.contributionToB
            )
        }
    }
}

data class RegretBreakdownResponse(
    val historicalRegretRate: Double,
    val valenceVariance: Double,
    val optionNegativityA: Double,
    val optionNegativityB: Double,
    val baseRegret: Double,
    val regretRiskA: Double,
    val regretRiskB: Double,
    val feedbackCount: Int,
    val dataReliability: String,
    val formula: String
) {
    companion object {
        fun from(regret: RegretBreakdown): RegretBreakdownResponse {
            return RegretBreakdownResponse(
                historicalRegretRate = regret.historicalRegretRate,
                valenceVariance = regret.valenceVariance,
                optionNegativityA = regret.optionNegativityA,
                optionNegativityB = regret.optionNegativityB,
                baseRegret = regret.baseRegret,
                regretRiskA = regret.regretRiskA,
                regretRiskB = regret.regretRiskB,
                feedbackCount = regret.feedbackCount,
                dataReliability = regret.dataReliability.name,
                formula = regret.formula
            )
        }
    }
}

data class CalculationParametersResponse(
    val lambda: Double,
    val regretPrior: Double,
    val priorityAxisBoost: Double,
    val volatilityWeight: Double,
    val negativityWeight: Double
) {
    companion object {
        fun from(params: CalculationParameters): CalculationParametersResponse {
            return CalculationParametersResponse(
                lambda = params.lambda,
                regretPrior = params.regretPrior,
                priorityAxisBoost = params.priorityAxisBoost,
                volatilityWeight = params.volatilityWeight,
                negativityWeight = params.negativityWeight
            )
        }
    }
}

data class ScoreBreakdownResponse(
    val scoreA: Double,
    val scoreB: Double,
    val formula: String
) {
    companion object {
        fun from(scores: ScoreBreakdown): ScoreBreakdownResponse {
            return ScoreBreakdownResponse(
                scoreA = scores.scoreA,
                scoreB = scores.scoreB,
                formula = scores.formula
            )
        }
    }
}
```

### 4.2 DecisionResponse 수정

**파일**: `pros-api/src/main/kotlin/com/aletheia/pros/api/dto/response/DecisionResponses.kt`

```kotlin
// DecisionResponse에 breakdown 필드 추가 (nullable)

data class DecisionResponse(
    val id: String,
    val title: String,
    val optionA: String,
    val optionB: String,
    val priorityAxis: String?,
    val result: DecisionResultResponse,
    val createdAt: Instant,

    // NEW: 계산 상세 (detail=true일 때만 포함)
    val breakdown: CalculationBreakdownResponse? = null
) {
    companion object {
        fun from(decision: Decision, includeBreakdown: Boolean = false): DecisionResponse {
            return DecisionResponse(
                id = decision.id.toString(),
                title = decision.title,
                optionA = decision.optionA,
                optionB = decision.optionB,
                priorityAxis = decision.priorityAxis?.name,
                result = DecisionResultResponse.from(decision),
                createdAt = decision.createdAt,
                breakdown = if (includeBreakdown) {
                    decision.result.breakdown?.let { CalculationBreakdownResponse.from(it) }
                } else null
            )
        }
    }
}
```

### 4.3 Controller 수정

**파일**: `pros-api/src/main/kotlin/com/aletheia/pros/api/controller/DecisionController.kt`

```kotlin
@GetMapping("/{id}")
@Operation(summary = "Get decision by ID")
fun getDecision(
    @PathVariable id: UUID,
    @RequestHeader("X-User-Id") userId: UUID,
    @RequestParam(defaultValue = "false") detail: Boolean  // NEW
): ResponseEntity<DecisionResponse> {
    val decision = queryDecisionUseCase.getById(DecisionId(id), UserId(userId))
    return ResponseEntity.ok(DecisionResponse.from(decision, includeBreakdown = detail))
}

@PostMapping
@Operation(summary = "Create a new decision projection")
fun createDecision(
    @RequestHeader("X-User-Id") userId: UUID,
    @Valid @RequestBody request: CreateDecisionRequest,
    @RequestParam(defaultValue = "false") detail: Boolean  // NEW
): ResponseEntity<DecisionResponse> {
    // ... 기존 로직 ...
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(DecisionResponse.from(decision, includeBreakdown = detail))
}
```

---

## 5. 구현 순서

| 순서 | 모듈 | 파일 | 작업 |
|------|------|------|------|
| 1 | pros-domain | `breakdown/FragmentContribution.kt` | 신규 생성 |
| 2 | pros-domain | `breakdown/FitBreakdown.kt` | 신규 생성 |
| 3 | pros-domain | `breakdown/RegretBreakdown.kt` | 신규 생성 |
| 4 | pros-domain | `breakdown/CalculationBreakdown.kt` | 신규 생성 |
| 5 | pros-domain | `Decision.kt` | breakdown 필드 추가 |
| 6 | pros-application | `CreateDecisionUseCase.kt` | breakdown 생성 로직 추가 |
| 7 | pros-api | `CalculationBreakdownResponse.kt` | 신규 생성 |
| 8 | pros-api | `DecisionResponses.kt` | breakdown 필드 추가 |
| 9 | pros-api | `DecisionController.kt` | detail 파라미터 추가 |
| 10 | - | 테스트 | 단위/통합 테스트 |

---

## 6. 테스트 계획

### 6.1 단위 테스트

```kotlin
// FitBreakdownTest.kt
@Test
fun `should calculate fit breakdown with contributions`() {
    val breakdown = FitBreakdown(
        fitScoreA = 0.6,
        fitScoreB = 0.4,
        totalWeight = 3.5,
        priorityAxisBoost = 0.35,
        fragmentContributions = listOf(...)
    )

    assertThat(breakdown.isOptionAMoreFit).isTrue()
    assertThat(breakdown.fitDifference).isEqualTo(0.2)
}

// RegretBreakdownTest.kt
@Test
fun `should determine data reliability based on feedback count`() {
    val lowReliability = RegretBreakdown(..., feedbackCount = 1)
    val highReliability = RegretBreakdown(..., feedbackCount = 15)

    assertThat(lowReliability.dataReliability).isEqualTo(DataReliability.LOW)
    assertThat(highReliability.dataReliability).isEqualTo(DataReliability.HIGH)
}
```

### 6.2 API 테스트

```bash
# 기존 응답 (하위호환)
curl http://localhost:8080/api/v1/decisions/{id} \
  -H "X-User-Id: {userId}"
# breakdown 필드 없음

# 상세 응답
curl "http://localhost:8080/api/v1/decisions/{id}?detail=true" \
  -H "X-User-Id: {userId}"
# breakdown 필드 포함
```

### 6.3 검증 항목

- [ ] 기존 API 응답 구조 유지 (하위호환)
- [ ] `?detail=true` 파라미터로 breakdown 포함
- [ ] breakdown 없이도 Decision 생성 가능
- [ ] 모든 중간값이 재현 가능
- [ ] FragmentContribution 요약 80자 제한

---

## 7. 호환성

| 항목 | 상태 |
|------|------|
| 기존 API 응답 | 유지 (breakdown=null) |
| 기존 DB 스키마 | 변경 없음 (breakdown은 계산시 생성) |
| 성능 영향 | 미미함 (기존 계산에 캡처만 추가) |
| 클라이언트 영향 | 없음 (선택적 필드) |

---

**작성일**: 2026-02-01
**상태**: Ready for Implementation
**다음 단계**: 구현 시작 → Phase 1 (Domain 모델)
