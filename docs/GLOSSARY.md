# PROS Domain Glossary (도메인 용어 사전)

> 이 문서는 PROS 프로젝트에서 사용하는 핵심 용어를 정의합니다.
> 모든 팀원이 동일한 언어로 소통하기 위한 기준 문서입니다.

---

## Core Concepts (핵심 개념)

### Thought Fragment (생각 파편)
- **정의**: 사용자가 특정 순간에 기록한 생각, 감정, 메모의 최소 단위
- **특성**: 불변(Immutable), Append-only
- **예시**: "오늘 회의에서 내 의견이 무시당한 느낌이다"
- **관련**: `moodValence`, `arousal`, `embedding`

### Value Axis (가치 축)
- **정의**: 인간의 핵심 가치를 분류하는 8개의 고정된 차원
- **목록**:
  | 코드 | 한글 | 영문 |
  |------|------|------|
  | `GROWTH` | 성장/학습 | Growth/Learning |
  | `STABILITY` | 안정/예측가능 | Stability/Predictability |
  | `FINANCIAL` | 금전/보상 | Financial/Reward |
  | `AUTONOMY` | 자율/통제 | Autonomy/Control |
  | `RELATIONSHIP` | 관계/소속 | Relationship/Belonging |
  | `ACHIEVEMENT` | 성취/인정 | Achievement/Recognition |
  | `HEALTH` | 건강/에너지 | Health/Energy |
  | `MEANING` | 의미/기여 | Meaning/Contribution |

### Value Graph (가치 그래프)
- **정의**: 사용자의 가치 축들 간의 관계를 나타내는 그래프 구조
- **구성요소**: `ValueNode`, `ValueEdge`
- **특성**: 모순(Conflict) 허용, 시간에 따른 변화 추적

### Decision Projection (결정 투영)
- **정의**: 과거 파편을 기반으로 현재 결정의 적합도를 계산하는 과정
- **출력**: 확률 + 후회 위험 + 근거 파편
- **원칙**: 추천 없음, 계산 결과만 제시

---

## Data Models (데이터 모델)

### Fragment ID
- **타입**: `UUID`
- **정의**: Thought Fragment의 고유 식별자

### User ID
- **타입**: `UUID`
- **정의**: 사용자의 고유 식별자

### Mood Valence (감정가)
- **타입**: `Double`
- **범위**: `-1.0` (매우 부정) ~ `+1.0` (매우 긍정)
- **정의**: 파편에 담긴 감정의 긍정/부정 정도
- **예시**:
  - `+0.8`: "승진 소식에 정말 기쁘다"
  - `-0.6`: "또 야근이라니 지친다"

### Arousal (각성도)
- **타입**: `Double`
- **범위**: `0.0` (낮음) ~ `1.0` (높음)
- **정의**: 감정의 강도 또는 활성화 수준
- **예시**:
  - `0.9`: 흥분, 분노, 환희
  - `0.2`: 우울, 평온, 무기력

### Embedding (임베딩)
- **타입**: `FloatArray` (dimension: 1536)
- **정의**: 텍스트의 의미를 수치 벡터로 변환한 것
- **용도**: 유사도 검색, 가치 축 매핑

---

## Value Graph Components (가치 그래프 구성요소)

### ValueNode (가치 노드)
- **정의**: 특정 가치 축의 상태를 나타내는 노드
- **속성**:
  - `axis`: 가치 축 종류
  - `avgValence`: 해당 가치에 대한 평균 감정가
  - `recentTrend`: 최근 추세
  - `fragmentCount`: 연관된 파편 수

### ValueEdge (가치 엣지)
- **정의**: 두 가치 축 간의 관계를 나타내는 연결
- **유형**:
  - `SUPPORT`: 두 가치가 서로 강화하는 관계
  - `CONFLICT`: 두 가치가 서로 충돌하는 관계
- **원칙**: Conflict edge는 삭제 금지 (모순 허용)

### Trend (추세)
- **정의**: 특정 가치에 대한 감정의 시간적 변화 방향
- **값**:
  - `RISING`: 상승 추세
  - `FALLING`: 하락 추세
  - `NEUTRAL`: 변화 없음

---

## Decision Components (결정 구성요소)

### Decision (결정)
- **정의**: 사용자가 투영을 요청한 A/B 선택 상황
- **구성**:
  - `title`: 결정 제목
  - `optionA`: 첫 번째 선택지
  - `optionB`: 두 번째 선택지
  - `priorityAxis`: 우선시할 가치 축 (선택)

### Probability (확률)
- **표기**: `P(A|Me)`, `P(B|Me)`
- **정의**: "나"의 과거 패턴 기준으로 각 선택이 적합할 확률
- **계산**: Softmax(scoreA, scoreB)
- **주의**: 이것은 "정답"이 아니라 "과거의 나 기준 적합도"

### Regret Risk (후회 위험)
- **정의**: 해당 선택 후 후회할 가능성의 추정치
- **범위**: `0.0` ~ `1.0`
- **계산요소**:
  - 과거 유사 결정의 후회율
  - 손실 회피 가중치 (λ)

### Lambda (λ, 후회 민감도)
- **정의**: 후회 위험에 대한 사용자의 민감도 가중치
- **초기값**: `1.0`
- **조정**: 피드백에 따라 자동 업데이트
- **수식**: `score = fit - λ * regretRisk`

### Evidence Fragments (근거 파편)
- **정의**: 결정 투영의 근거가 된 과거 파편들 (Top 5)
- **선정 기준**:
  - Context embedding과의 유사도
  - Mood-congruent 가중치

---

## Processing Concepts (처리 개념)

### Soft Assignment (부드러운 할당)
- **정의**: 파편을 여러 가치 축에 확률적으로 할당하는 방식
- **특성**: 합이 1이 아님, 다중 귀속 허용
- **예시**:
  ```
  Fragment: "새 프로젝트 리더가 되어 기쁘지만 책임감이 무겁다"
  → ACHIEVEMENT: 0.8
  → GROWTH: 0.6
  → STABILITY: -0.3 (부정적 연관)
  ```

### Mood-Congruent Weighting (기분 일치 가중)
- **정의**: 현재 감정 상태와 유사한 감정의 파편에 더 높은 가중치를 부여
- **이론**: 기분 일치 기억 효과 (Mood-Congruent Memory)
- **용도**: 유사 파편 검색 시 적용

### Similarity Search (유사도 검색)
- **정의**: 벡터 공간에서 가장 가까운 파편들을 찾는 과정
- **알고리즘**: HNSW (Hierarchical Navigable Small World)
- **지표**: Cosine Similarity

---

## Feedback Concepts (피드백 개념)

### Feedback Type (피드백 유형)
- **정의**: 결정 후 사용자가 제공하는 만족도 평가
- **값**:
  - `SATISFIED`: 만족 (좋은 결정이었다)
  - `NEUTRAL`: 보통 (괜찮았다)
  - `REGRET`: 후회 (다르게 할 걸)

### Feedback Loop (피드백 루프)
- **정의**: 사용자 피드백을 시스템 학습에 반영하는 순환 구조
- **주기**: 결정 후 24~72시간
- **조정 대상**: λ, 가치 축 가중치, regret prior

---

## System Boundaries (시스템 경계)

### Explanation Layer (설명 계층)
- **정의**: LLM을 사용하여 계산 결과를 자연어로 변환하는 계층
- **제약**: 설명만 가능, 판단/추천 금지

### Calculation Engine (계산 엔진)
- **정의**: 확률, 적합도, 후회위험을 수학적으로 계산하는 엔진
- **특성**: 결정론적, 재현 가능, 테스트 가능

---

## Anti-Patterns (안티패턴)

### Recommendation (추천)
- **정의**: "A를 선택하세요" 같은 판단적 출력
- **상태**: ❌ 금지

### Value Judgment (가치 판단)
- **정의**: 사용자의 가치관에 대한 평가
- **상태**: ❌ 금지

### Goal Setting (목표 설정)
- **정의**: 시스템이 사용자에게 목표를 제안하는 것
- **상태**: ❌ 금지

### Conflict Resolution (충돌 해결)
- **정의**: 가치 그래프의 모순을 자동으로 제거하는 것
- **상태**: ❌ 금지

---

## Abbreviations (약어)

| 약어 | 전체 | 의미 |
|------|------|------|
| PROS | Personal Reasoning OS | 개인 추론 운영체제 |
| MCP | Model Context Protocol | 모델 컨텍스트 프로토콜 |
| LLM | Large Language Model | 대규모 언어 모델 |
| HNSW | Hierarchical Navigable Small World | 벡터 검색 알고리즘 |

---

## Version History

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2025-01 | 초기 용어 정의 |
