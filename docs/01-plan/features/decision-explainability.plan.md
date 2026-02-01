# Plan: Decision Explainability 개선

> Decision API 응답에 계산 근거를 포함하여 투명성 확보

## 1. 문제 정의

### 현재 상황
Decision API 응답에서 `regretRiskA`, `regretRiskB`, `probabilityA`, `probabilityB` 값이 반환되지만, 이 수치가 어떻게 산정되었는지 사용자가 이해할 수 없음.

```json
{
  "result": {
    "probabilityA": 51,
    "probabilityB": 48,
    "regretRiskA": 22,
    "regretRiskB": 22
  }
}
```

### 근본 원인 분석
1. **계산 과정 비공개**: 내부 중간값(fitA, fitB, lambda 등)이 응답에 포함되지 않음
2. **근거 데이터 불투명**: 어떤 fragment들이 얼마나 영향을 주었는지 불분명
3. **매직넘버 사용**: 0.3, 0.5 같은 가중치 상수의 의미 불명확
4. **동일 값 발생 빈도**: regretRiskA와 regretRiskB가 같은 경우가 많아 차별화 근거 필요

### 영향 범위
- 사용자: 결과에 대한 신뢰도 저하
- 개발자: 디버깅 및 알고리즘 검증 어려움
- 제품: "설명 가능한 AI" 원칙 위배

## 2. 목표

### 핵심 목표
1. **계산 근거 투명화**: 모든 중간 계산값을 API 응답에 포함
2. **기여도 명시**: 각 fragment가 결과에 미친 영향도 수치화
3. **파라미터 가시화**: lambda, prior 등 설정값 노출
4. **사용자 이해 지원**: 자연어 설명 생성 근거 제공

### 성공 지표
- [ ] API 응답에 계산 breakdown 포함
- [ ] 각 evidence fragment의 기여도 점수 포함
- [ ] regretRisk 구성 요소별 수치 분리
- [ ] 기존 클라이언트 하위호환 유지

## 3. 범위

### In Scope
1. `DecisionResult`에 계산 breakdown 필드 추가
2. Evidence fragment별 contribution score 계산
3. Regret risk 구성요소 분리 (historical, volatility, negativity)
4. API 응답 DTO 확장
5. 기존 응답 구조 하위호환 유지

### Out of Scope
- 자연어 설명 생성 (기존 ExplanationUseCase 담당)
- UI 시각화
- 계산 알고리즘 자체 변경

## 4. 기술 분석

### 현재 계산 흐름
```
CreateDecisionUseCase.execute()
    │
    ├─ calculateValueFit() → fitA, fitB (중간값, 미노출)
    │
    ├─ calculateRegretRisk() → regretA, regretB (중간값, 미노출)
    │
    └─ DecisionResult.compute(fitA, fitB, regretA, regretB, lambda)
           │
           └─ 최종 확률만 반환
```

### 개선 후 구조
```
CreateDecisionUseCase.execute()
    │
    ├─ calculateValueFit()
    │      └─ FitBreakdown (fitA, fitB, fragmentContributions[])
    │
    ├─ calculateRegretRisk()
    │      └─ RegretBreakdown (historicalRate, volatility, negativityA, negativityB)
    │
    └─ DecisionResult.compute()
           └─ DecisionResult with:
               - probabilities
               - regretRisks
               - calculationBreakdown {
                   fit: FitBreakdown,
                   regret: RegretBreakdown,
                   parameters: { lambda, regretPrior }
                 }
```

### 필요 컴포넌트

| 컴포넌트 | 위치 | 역할 |
|---------|------|------|
| `FitBreakdown` | pros-domain | 적합도 계산 상세 |
| `RegretBreakdown` | pros-domain | 후회 위험 계산 상세 |
| `FragmentContribution` | pros-domain | fragment별 기여도 |
| `CalculationBreakdown` | pros-domain | 전체 계산 상세 집합 |
| `DecisionDetailResponse` | pros-api | 확장된 API 응답 DTO |

### 주요 필드 정의

#### FitBreakdown
```kotlin
data class FitBreakdown(
    val fitScoreA: Double,        // 옵션A 적합도 (0.0~1.0)
    val fitScoreB: Double,        // 옵션B 적합도 (0.0~1.0)
    val totalWeight: Double,      // 총 가중치
    val priorityAxisBoost: Double, // 우선축 부스트 (0.35)
    val fragmentContributions: List<FragmentContribution>
)

data class FragmentContribution(
    val fragmentId: UUID,
    val fragmentText: String,     // 요약 텍스트 (50자 제한)
    val similarity: Double,       // 의미적 유사도
    val valenceWeight: Double,    // 감정 가중치
    val contributionToA: Double,  // 옵션A 기여도
    val contributionToB: Double   // 옵션B 기여도
)
```

#### RegretBreakdown
```kotlin
data class RegretBreakdown(
    val historicalRegretRate: Double,  // 과거 피드백 기반 (0.0~1.0)
    val valenceVariance: Double,       // 감정 분산 (불확실성)
    val optionNegativityA: Double,     // 옵션A 부정성
    val optionNegativityB: Double,     // 옵션B 부정성
    val baseRegret: Double,            // 기본 후회값
    val feedbackCount: Int,            // 피드백 샘플 수
    val formula: String                // "baseRegret + (negativity - 0.5) × 0.3"
)
```

#### Parameters
```kotlin
data class CalculationParameters(
    val lambda: Double,          // 후회 민감도 (기본 1.0)
    val regretPrior: Double,     // 기본 후회율 (기본 0.2)
    val priorityAxisBoost: Double, // 0.35
    val volatilityWeight: Double,  // 0.3
    val negativityWeight: Double   // 0.3
)
```

## 5. API 응답 구조

### 기존 응답 (하위호환 유지)
```json
{
  "id": "uuid",
  "title": "...",
  "result": {
    "probabilityA": 51,
    "probabilityB": 48,
    "regretRiskA": 22,
    "regretRiskB": 22
  }
}
```

### 확장 응답 (?detail=true 파라미터)
```json
{
  "id": "uuid",
  "title": "...",
  "result": {
    "probabilityA": 51,
    "probabilityB": 48,
    "regretRiskA": 22,
    "regretRiskB": 22
  },
  "breakdown": {
    "fit": {
      "fitScoreA": 0.52,
      "fitScoreB": 0.48,
      "totalWeight": 3.45,
      "priorityAxisBoost": 0.35,
      "fragmentContributions": [
        {
          "fragmentId": "uuid",
          "fragmentText": "AI와 함께 일하면서 생산성이...",
          "similarity": 0.85,
          "valenceWeight": 0.72,
          "contributionToA": 0.18,
          "contributionToB": 0.12
        }
      ]
    },
    "regret": {
      "historicalRegretRate": 0.2,
      "valenceVariance": 0.15,
      "optionNegativityA": 0.48,
      "optionNegativityB": 0.52,
      "baseRegret": 0.245,
      "feedbackCount": 0,
      "formula": "baseRegret + (negativity - 0.5) × 0.3"
    },
    "parameters": {
      "lambda": 1.0,
      "regretPrior": 0.2,
      "priorityAxisBoost": 0.35,
      "volatilityWeight": 0.3,
      "negativityWeight": 0.3
    },
    "scores": {
      "scoreA": 0.298,
      "scoreB": 0.262,
      "formula": "score = fit - lambda × regret"
    }
  }
}
```

## 6. 구현 우선순위

### Phase 1: Domain 모델 확장 (필수)
1. `FitBreakdown`, `RegretBreakdown`, `FragmentContribution` 값객체 추가
2. `CalculationBreakdown` 집합 객체 추가
3. `DecisionResult`에 breakdown 필드 추가 (nullable)

### Phase 2: Use Case 수정 (필수)
4. `CreateDecisionUseCase`에서 breakdown 생성 로직 추가
5. 기존 계산 로직에서 중간값 캡처

### Phase 3: API 확장 (필수)
6. `DecisionDetailResponse` DTO 추가
7. Controller에 `?detail=true` 쿼리 파라미터 지원
8. 기존 응답 하위호환 유지

### Phase 4: 테스트 (필수)
9. Domain 모델 단위 테스트
10. Use Case 통합 테스트
11. API 응답 검증 테스트

## 7. 리스크

| 리스크 | 영향 | 대응 |
|--------|------|------|
| 응답 크기 증가 | 네트워크 비용 | detail 파라미터로 선택적 포함 |
| 계산 오버헤드 | 성능 | breakdown 계산은 기존 계산과 동시 수행 |
| 하위호환 깨짐 | 기존 클라이언트 | 기존 응답 구조 유지, 확장만 추가 |
| 민감 데이터 노출 | 개인정보 | fragmentText 50자 제한, 필요시 마스킹 |

## 8. 설계 원칙 준수 확인

| 원칙 | 준수 여부 | 설명 |
|------|----------|------|
| LLM 사용 금지 (계산) | O | breakdown은 순수 계산값, LLM 미사용 |
| 불변성 | O | DecisionResult는 생성 후 변경 불가 |
| 계산/설명 분리 | O | 수치만 제공, 자연어 설명은 별도 API |
| 사용자 에이전시 | O | 근거 투명화로 사용자 판단 지원 |

## 9. 일정

- **Phase 1**: Domain 모델 확장
- **Phase 2**: Use Case 수정
- **Phase 3**: API 확장
- **Phase 4**: 테스트

---

**작성일**: 2026-02-01
**상태**: Draft
**다음 단계**: Design 문서 작성 → `/pdca design decision-explainability`
