# Plan: ValueAlignment 사용자 가치 데이터 반영 개선

> **Feature ID**: value-alignment-improvement
> **Version**: 1.0
> **Created**: 2026-02-01
> **Status**: Draft

---

## 1. 문제 정의

### 1.1 현재 상태 분석

현재 `valueAlignment` 값은 Decision 생성 시 다음 방식으로 계산됩니다:

```
CreateDecisionUseCase.calculateValueAlignment():

1. 각 ValueAxis(8개)에 대해:
   - axisText = "Value: Growth/Learning. The drive to learn..."
   - axisEmbedding = embeddingPort.embed(axisText)

2. 코사인 유사도 계산:
   - simA = optionA.cosineSimilarity(axisEmbedding)
   - simB = optionB.cosineSimilarity(axisEmbedding)

3. 차등 정렬 계산:
   - alignment = (simA - simB + 1.0) / 2.0

4. 사용자 가중치 적용 (선택적):
   - if (node.fragmentCount > 0):
     - deviation * userWeight(avgValence) * 2.0
```

### 1.2 핵심 문제점

| 문제 | 현재 상태 | 영향도 |
|------|----------|--------|
| **사용자 명시적 가치 입력 없음** | avgValence만 간접 사용 | 🔴 High |
| **가치 중요도 미수집** | 8개 축 동일 가중치 | 🔴 High |
| **ValueNode 자동 생성** | Fragment에서 추론만 | 🟡 Medium |
| **값 범위 집중** | 0.48~0.54 범위 몰림 (구분력 부족) | 🟡 Medium |

### 1.3 실제 API 응답 예시

```json
{
  "valueAlignment": {
    "GROWTH": 0.5448062378867499,
    "HEALTH": 0.47181293154748805,
    "MEANING": 0.52038454132688,
    "AUTONOMY": 0.5304288286666299,
    "FINANCIAL": 0.48184596511300487,
    "STABILITY": 0.5014747607424656,
    "ACHIEVEMENT": 0.4944857035024766,
    "RELATIONSHIP": 0.4819365221187276
  }
}
```

**문제**: 모든 값이 0.5 주변에 몰려있어 옵션 간 가치 차이가 거의 보이지 않음.

---

## 2. 목표

### 2.1 핵심 목표

1. **사용자 가치 중요도 명시적 수집**: 8개 ValueAxis에 대한 개인별 중요도 설정
2. **가치 정렬 계산에 사용자 데이터 반영**: 수집된 가치 데이터를 valueAlignment 계산에 통합
3. **값 구분력 향상**: 0.5 주변 집중 현상 해소

### 2.2 비목표 (Out of Scope)

- ValueAxis 개수 변경 (8개 고정 유지)
- LLM을 이용한 가치 계산 (DECISION_PRINCIPLES.md 위반)
- 가치 기반 "추천" 기능 (프로젝트 철학 위반)

---

## 3. 외부 조사 및 Best Practice

### 3.1 학술적 배경: Schwartz 가치 이론

[Schwartz Theory of Basic Human Values](https://en.wikipedia.org/wiki/Theory_of_basic_human_values)는 인간의 기본 가치를 체계적으로 분류한 대표적 이론입니다.

**핵심 원리**:
- 가치는 상대적 중요도로 순위가 매겨짐
- 가치 간 trade-off가 행동을 결정
- 가치는 원형 구조로 배열되어 인접 가치는 호환, 대각 가치는 충돌

**측정 방법**:
- 각 가치 항목에 대해 "나의 삶을 이끄는 원칙으로서 얼마나 중요한가" 평가
- 7점 또는 9점 척도 사용
- Within-individual mean centering으로 개인 간 비교 가능

### 3.2 현대 가치 측정 도구

| 도구 | 접근 방식 | 적용 가능성 |
|------|----------|-------------|
| [The Values Bridge](https://thevaluesbridge.com/) | 16개 가치의 Authenticity Score | 🟢 높음 |
| [Life Values Inventory](https://positivepsychology.com/values-questionnaire/) | 5개 생활 영역별 측정 | 🟡 중간 |
| [Personal Values Assessment](https://personalvalu.es) | 구조화된 비교 기반 우선순위 | 🟢 높음 |

### 3.3 핵심 인사이트

1. **Authenticity Score 개념**: "내가 중요하게 여기는 것"과 "실제로 표현하는 것"의 Gap 측정
2. **구조화된 비교**: 단순 점수 부여보다 가치 간 비교가 더 정확한 우선순위 도출
3. **주기적 재평가**: 가치관은 시간에 따라 변하므로 정기적 업데이트 필요

---

## 4. 제안 솔루션

### 4.1 다층 가치 데이터 수집 전략

```
┌─────────────────────────────────────────────────────────────┐
│                    가치 데이터 레이어                        │
├─────────────────────────────────────────────────────────────┤
│ Layer 1: 명시적 중요도 (Explicit Importance)                 │
│  - 사용자가 직접 각 축의 중요도를 1-10 점수로 지정           │
│  - 온보딩 시 초기 설정 + 주기적 재평가                       │
├─────────────────────────────────────────────────────────────┤
│ Layer 2: 암묵적 중요도 (Implicit Importance)                 │
│  - ThoughtFragment 분석에서 자동 추론                        │
│  - 기존 avgValence + fragmentCount 기반                      │
├─────────────────────────────────────────────────────────────┤
│ Layer 3: 행동 기반 중요도 (Behavioral Importance)            │
│  - Decision 피드백에서 역추론                                │
│  - 만족/후회 패턴 분석                                       │
├─────────────────────────────────────────────────────────────┤
│ Layer 4: 맥락별 가중치 (Contextual Weight)                   │
│  - 결정 유형별 가치 중요도 차등 적용                         │
│  - 예: 직업 결정 시 FINANCIAL/GROWTH 가중치 상승             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 개선된 valueAlignment 계산 공식

**현재**:
```
alignment = (simA - simB + 1.0) / 2.0
if (fragmentCount > 0):
    alignment = 0.5 + (deviation * avgValence * 2.0)
```

**제안**:
```
// 1. 기본 유사도 계산 (현재와 동일)
baseDiff = simA - simB  // -1.0 ~ 1.0

// 2. 사용자 가치 중요도 적용 (신규)
importance = userValueProfile.getImportance(axis)  // 0.0 ~ 1.0
amplifiedDiff = baseDiff * (1.0 + importance)  // 중요한 축은 차이 확대

// 3. 암묵적 가중치 적용 (기존 로직 개선)
if (fragmentCount > 0):
    implicitWeight = normalize(avgValence, fragmentCount)
    amplifiedDiff *= (1.0 + implicitWeight * 0.5)

// 4. 정규화
alignment = (amplifiedDiff + MAX_DIFF) / (2 * MAX_DIFF)
alignment = alignment.coerceIn(0.0, 1.0)
```

### 4.3 데이터 흐름

```
┌──────────────────────────────────────────────────────────────────┐
│                        사용자 입력                                │
├──────────────────────────────────────────────────────────────────┤
│  1. 온보딩 설문                                                   │
│     "다음 가치들이 당신의 삶에서 얼마나 중요한가요?"              │
│     GROWTH [1━━━━━━━━━━━10]                                       │
│     STABILITY [1━━━━━━━━━━━10]                                    │
│     ...                                                           │
├──────────────────────────────────────────────────────────────────┤
│  2. ThoughtFragment 입력 (기존)                                   │
│     → 자동으로 관련 ValueAxis 감지 및 가중치 업데이트             │
├──────────────────────────────────────────────────────────────────┤
│  3. Decision 피드백 (기존)                                        │
│     → 만족/후회 패턴으로 가치 예측 정확도 학습                    │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                     UserValueProfile (신규)                       │
├──────────────────────────────────────────────────────────────────┤
│  {                                                                │
│    userId: "user-123",                                            │
│    explicitImportance: {                                          │
│      GROWTH: 0.9,                                                 │
│      STABILITY: 0.4,                                              │
│      FINANCIAL: 0.7,                                              │
│      AUTONOMY: 0.8,                                               │
│      RELATIONSHIP: 0.6,                                           │
│      ACHIEVEMENT: 0.5,                                            │
│      HEALTH: 0.7,                                                 │
│      MEANING: 0.9                                                 │
│    },                                                             │
│    lastUpdated: "2026-01-31",                                     │
│    version: 3                                                     │
│  }                                                                │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│                  calculateValueAlignment()                        │
├──────────────────────────────────────────────────────────────────┤
│  Input:                                                           │
│    - optionAEmbedding, optionBEmbedding                           │
│    - valueGraph.nodes (기존)                                      │
│    - userValueProfile.explicitImportance (신규)                   │
│                                                                   │
│  Output:                                                          │
│    Map<ValueAxis, Double> with enhanced differentiation           │
└──────────────────────────────────────────────────────────────────┘
```

---

## 5. 검증 기준

### 5.1 기능 요구사항

| ID | 요구사항 | 검증 방법 |
|----|---------|----------|
| FR-1 | 사용자가 8개 ValueAxis별 중요도를 설정할 수 있다 | API 테스트 |
| FR-2 | 설정된 중요도가 valueAlignment 계산에 반영된다 | 단위 테스트 |
| FR-3 | 중요도 설정 없이도 기존 로직이 동작한다 (하위호환) | 통합 테스트 |
| FR-4 | 중요도를 업데이트할 수 있다 | API 테스트 |

### 5.2 성능 요구사항

| ID | 요구사항 | 목표값 |
|----|---------|-------|
| NFR-1 | valueAlignment 계산 시간 | < 100ms 추가 |
| NFR-2 | 값 분산도 (표준편차) | > 0.15 (현재 ~0.03) |

### 5.3 설계 원칙 준수

| 원칙 | 검증 항목 |
|------|----------|
| LLM 금지 영역 준수 | 중요도 계산에 LLM 미사용 확인 |
| 데이터 불변성 | 중요도 변경 시 이력 보존 확인 |
| 추천 금지 | valueAlignment은 정보 제공만, 추천 문구 없음 |

---

## 6. 위험 및 고려사항

### 6.1 기술적 위험

| 위험 | 완화 방안 |
|------|----------|
| 기존 API 호환성 깨짐 | explicitImportance 없으면 기존 로직 fallback |
| 온보딩 이탈률 증가 | 중요도 설정을 선택적으로 제공, 기본값 활용 |
| 계산 복잡도 증가 | 중요도는 캐시하여 매번 조회하지 않음 |

### 6.2 사용자 경험 위험

| 위험 | 완화 방안 |
|------|----------|
| 설문 피로도 | 최소 질문 수 (8개)로 제한 |
| 가치관 변화 미반영 | 주기적 재평가 알림 + 명시적 업데이트 기능 |
| 과도한 개인화 | 전체 중요도 리셋 기능 제공 |

---

## 7. 구현 우선순위

### Phase 1: 기반 구축 (필수)
1. UserValueProfile 도메인 모델 생성
2. 명시적 중요도 저장/조회 API
3. calculateValueAlignment 로직 개선

### Phase 2: 사용자 경험 (권장)
4. 온보딩 플로우에 가치 설문 통합
5. 가치 프로필 대시보드 UI

### Phase 3: 고도화 (선택)
6. Decision 피드백 기반 가치 학습
7. 맥락별 가중치 자동 조정

---

## 8. 관련 문서

- [DECISION_PRINCIPLES.md](/docs/DECISION_PRINCIPLES.md) - 설계 원칙
- [theoretical-background.md](/docs/theoretical-background.md) - 이론적 배경
- [GLOSSARY.md](/docs/GLOSSARY.md) - 용어 정의

---

## 9. 승인

| 역할 | 이름 | 승인일 |
|------|------|-------|
| Product Owner | - | - |
| Tech Lead | - | - |

---

*Generated by PDCA Process*
