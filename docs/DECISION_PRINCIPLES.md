# PROS Decision Principles

> 이 문서는 PROS 시스템의 핵심 설계 원칙을 정의합니다.
> 모든 기능 개발 전 이 원칙에 부합하는지 반드시 검토해야 합니다.

## 핵심 철학

```
"이 프로젝트는 '결정을 대신하는 AI'가 아니라
 '과거의 나를 호출해주는 시스템'을 만드는 일이다."
```

---

## 1. LLM 사용 원칙

### 1.1 LLM 금지 영역 (❌ NEVER)

| 금지 행위 | 이유 | 대안 |
|-----------|------|------|
| 판단 생성 | 사용자의 결정권 침해 | 확률만 제시 |
| 추천/조언 | "더 나은" 선택이라는 개념 자체가 주관적 | 근거만 제시 |
| 목표 제안 | 사용자가 정의해야 할 영역 | 과거 패턴만 보여줌 |
| 가치 평가 | 모순도 사용자의 일부 | 충돌을 있는 그대로 표시 |
| 감정 판단 | "당신은 슬퍼 보입니다" 같은 진단 | 감정 점수만 계산 |

### 1.2 LLM 허용 영역 (✅ ALLOWED)

| 허용 행위 | 조건 | 예시 |
|-----------|------|------|
| 결과 설명 | 계산 과정 해설만 | "이 확률은 최근 3개월 파편 기준입니다" |
| 파편 요약 | 원문 변경 없이 | "5개 근거 파편의 공통 키워드는..." |
| 질문 생성 | 정보 수집 목적만 | "이 결정에서 중요한 가치가 있나요?" |
| 형식 변환 | 구조화만 | JSON → 자연어 변환 |

### 1.3 LLM 프롬프트 가드레일

모든 LLM 호출에는 다음 시스템 프롬프트가 포함되어야 합니다:

```
[SYSTEM CONSTRAINTS]
You are an explanation assistant for PROS (Personal Reasoning OS).

STRICT RULES:
1. NEVER recommend, suggest, or advise any choice
2. NEVER use phrases like "you should", "I recommend", "better option"
3. NEVER judge user's values or emotions
4. ONLY explain WHY the calculation produced these results
5. ONLY summarize the evidence fragments
6. ALWAYS use neutral, descriptive language

Your role is to TRANSLATE calculations into human-readable explanations.
You do NOT make decisions. The user makes decisions.
```

---

## 2. 데이터 불변성 원칙

### 2.1 Append-Only Memory

```kotlin
// ❌ FORBIDDEN
fun updateFragment(id: FragmentId, newText: String)

// ✅ ALLOWED
fun createFragment(text: String): Fragment
fun softDeleteFragment(id: FragmentId)
```

**이유:** 과거의 생각은 그 시점의 "나"를 대표합니다. 수정하면 과거의 나를 왜곡하게 됩니다.

### 2.2 Value Graph 모순 허용

```kotlin
// ❌ FORBIDDEN - 모순 자동 해결
if (hasConflict(GROWTH, STABILITY)) {
    removeWeakerEdge()  // 절대 금지
}

// ✅ ALLOWED - 모순 그대로 유지
data class ValueEdge(
    val type: EdgeType  // SUPPORT or CONFLICT 모두 허용
)
```

**이유:** 인간의 가치관은 본질적으로 모순을 포함합니다. "성장을 원하면서 안정도 원하는" 것은 정상입니다.

---

## 3. 계산과 설명의 분리

### 3.1 분리 원칙

```
[Calculation Engine]          [Explanation Layer]
     (수학적)        →              (언어적)
        ↓                            ↓
   결정론적 결과              자연어 해설
   (재현 가능)               (LLM 생성)
```

### 3.2 구현 규칙

| 계층 | 담당 | 특성 |
|------|------|------|
| Calculation Engine | 확률, 적합도, 후회위험 계산 | 결정론적, 테스트 가능 |
| Explanation Layer | 결과를 자연어로 변환 | LLM 사용, 판단 금지 |

```kotlin
// ✅ CORRECT: 계산과 설명 분리
class DecisionProjectionEngine {
    fun calculate(decision: Decision): DecisionResult  // 순수 계산
}

class ExplanationService {
    fun explain(result: DecisionResult): String  // LLM으로 설명만
}
```

---

## 4. 결과 표시 원칙

### 4.1 필수 출력 형식

모든 Decision 결과는 다음을 포함해야 합니다:

```json
{
  "probabilityA": 0.62,
  "probabilityB": 0.38,
  "regretRiskA": 0.15,
  "regretRiskB": 0.28,
  "evidenceFragments": [
    { "id": "...", "text": "...", "relevance": 0.89 }
  ],
  "explanation": "이 결과는 ... 에 기반합니다. (판단 없음)"
}
```

### 4.2 금지된 출력 형식

```json
// ❌ FORBIDDEN
{
  "recommendation": "A를 선택하세요",
  "betterOption": "A",
  "advice": "A가 당신에게 더 좋을 것 같습니다"
}
```

---

## 5. 피드백 처리 원칙

### 5.1 학습 범위

피드백으로 조정 가능한 것:
- λ (후회 민감도 가중치)
- 가치 축 가중치
- regret risk prior

피드백으로 조정 불가능한 것:
- 과거 파편 내용
- 가치 축 정의
- 확률 계산 공식

### 5.2 피드백 옵션

```kotlin
enum class FeedbackType {
    SATISFIED,  // 만족
    NEUTRAL,    // 보통
    REGRET      // 후회
}
```

3가지 옵션만 제공합니다. 상세한 피드백은 MVP 이후 고려합니다.

---

## 6. UX 원칙

### 6.1 최소 개입

```
[일상 모드]
- 파편 입력만
- 가치 그래프 표시 없음
- 판단 없음

[결정 모드]
- 결정 입력
- 확률 + 근거 제시
- 판단 없음
```

### 6.2 금지된 UX 패턴

| 패턴 | 문제 |
|------|------|
| "오늘의 목표" 설정 | 목표 강요 |
| "습관 트래커" | 행동 강제 |
| "더 나은 선택" 배지 | 암묵적 판단 |
| "당신의 결정 점수" | 평가적 표현 |

---

## 7. 체크리스트

### 7.1 기능 개발 전 체크

- [ ] 이 기능이 사용자의 결정을 대신하는가? → 금지
- [ ] 이 기능이 LLM으로 판단을 생성하는가? → 금지
- [ ] 이 기능이 과거 파편을 수정하는가? → 금지
- [ ] 이 기능이 가치 모순을 해결하려 하는가? → 금지

### 7.2 코드 리뷰 체크

- [ ] LLM 프롬프트에 가드레일이 있는가?
- [ ] 계산과 설명이 분리되어 있는가?
- [ ] 결과에 "추천/조언" 언어가 없는가?
- [ ] 데이터 불변성이 지켜지는가?

---

## 8. 위반 사례와 수정

### 8.1 위반 사례

```kotlin
// ❌ VIOLATION: LLM이 추천 생성
val prompt = "사용자의 상황을 분석하고 더 나은 선택을 추천해줘"

// ❌ VIOLATION: 모순 자동 해결
fun resolveConflict(edge: ValueEdge) {
    if (edge.type == CONFLICT) deleteEdge(edge)
}

// ❌ VIOLATION: 판단적 언어
fun formatResult(result: DecisionResult): String {
    return "A가 더 좋은 선택입니다 (${result.probabilityA}%)"
}
```

### 8.2 수정 사례

```kotlin
// ✅ CORRECT: LLM은 설명만
val prompt = """
    다음 계산 결과를 설명해줘. 추천하지 마.
    결과: ${result.toJson()}
"""

// ✅ CORRECT: 모순 유지
fun addEdge(edge: ValueEdge) {
    // CONFLICT도 그대로 저장
    repository.save(edge)
}

// ✅ CORRECT: 중립적 언어
fun formatResult(result: DecisionResult): String {
    return "A 선택 시 적합도: ${result.probabilityA}%"
}
```

---

## 변경 이력

| 날짜 | 버전 | 변경 내용 |
|------|------|----------|
| 2025-01 | 1.0 | 초기 원칙 정립 |
