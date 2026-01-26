# PROS (Personal Reasoning OS) Architecture

> "이 프로젝트는 '결정을 대신하는 AI'가 아니라 '과거의 나를 호출해주는 시스템'을 만드는 일이다."

## 1. System Overview

PROS는 사용자의 생각 파편(Thought Fragment)을 수집하고, 이를 기반으로 현재 결정이 "나다운지"와 "후회 위험"을 확률로 제시하는 개인 반추 OS입니다.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PROS Architecture                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐                │
│  │   Fragment   │────▶│    Value     │────▶│   Decision   │                │
│  │    Store     │     │    Graph     │     │  Projection  │                │
│  │              │     │   Engine     │     │    Engine    │                │
│  └──────────────┘     └──────────────┘     └──────────────┘                │
│         │                    │                    │                         │
│         │                    │                    │                         │
│         ▼                    ▼                    ▼                         │
│  ┌─────────────────────────────────────────────────────────┐               │
│  │                    PostgreSQL + pgvector                 │               │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────────────┐ │               │
│  │  │ fragments  │  │value_nodes │  │    decisions       │ │               │
│  │  │(append-only│  │value_edges │  │decision_feedbacks  │ │               │
│  │  └────────────┘  └────────────┘  └────────────────────┘ │               │
│  └─────────────────────────────────────────────────────────┘               │
│                              │                                              │
│                              ▼                                              │
│  ┌─────────────────────────────────────────────────────────┐               │
│  │              LLM Explanation Layer (MCP)                 │               │
│  │         ⚠️ 설명 전용 - 판단/추천 금지 ⚠️                  │               │
│  └─────────────────────────────────────────────────────────┘               │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 2. Technology Stack

### Core Framework
| Component | Technology | Version | Purpose |
|-----------|------------|---------|---------|
| Language | Kotlin | 2.0.x | Primary development language |
| Framework | Spring Boot | 3.4.x | Application framework |
| AI Integration | Spring AI | 1.0.x | LLM & embedding integration |
| MCP | Spring AI MCP | 1.0.x | Model Context Protocol for LLM |

### Data Layer
| Component | Technology | Version | Purpose |
|-----------|------------|---------|---------|
| Database | PostgreSQL | 17.x | Primary data store |
| Vector Extension | pgvector | 0.7.x | Vector similarity search |
| ORM | Spring Data JPA | 3.4.x | Data access layer |
| Migration | Flyway | 10.x | Database versioning |

### Infrastructure
| Component | Technology | Purpose |
|-----------|------------|---------|
| Build Tool | Gradle (Kotlin DSL) | Project build & dependency management |
| Container | Docker Compose | Local development environment |
| API Docs | SpringDoc OpenAPI | API documentation |

## 3. Module Architecture

```
pros-core/
├── pros-api/                    # REST API Layer
│   ├── fragment/               # Thought Fragment endpoints
│   ├── decision/               # Decision projection endpoints
│   └── feedback/               # Feedback collection endpoints
│
├── pros-domain/                 # Domain Layer (Pure Kotlin)
│   ├── fragment/               # ThoughtFragment aggregate
│   ├── value/                  # ValueGraph aggregate
│   ├── decision/               # Decision aggregate
│   └── common/                 # Shared value objects
│
├── pros-application/            # Application Layer
│   ├── fragment/               # Fragment use cases
│   ├── value/                  # Value graph use cases
│   ├── decision/               # Decision projection use cases
│   └── explanation/            # LLM explanation use cases
│
├── pros-infrastructure/         # Infrastructure Layer
│   ├── persistence/            # JPA repositories & entities
│   ├── embedding/              # Embedding service adapters
│   ├── llm/                    # LLM service adapters (MCP)
│   └── config/                 # Spring configurations
│
└── pros-bootstrap/              # Application Entry Point
    └── ProsApplication.kt
```

## 4. Core Components

### 4.1 Thought Fragment Store

생각 파편을 저장하고 관리하는 핵심 컴포넌트입니다.

**핵심 원칙:**
- Append-only: 한번 저장된 파편은 수정 불가
- Soft-delete only: 삭제는 논리적 삭제만 허용
- Immutable text: `textRaw`는 절대 변경 불가

```kotlin
data class ThoughtFragment(
    val id: FragmentId,
    val userId: UserId,
    val textRaw: String,           // 불변
    val createdAt: Instant,
    val moodValence: Double,       // -1.0 ~ +1.0
    val arousal: Double,           // 0.0 ~ 1.0
    val topicHint: String?,
    val embedding: FloatArray,     // 벡터 임베딩
    val deletedAt: Instant? = null // soft-delete
)
```

### 4.2 Value Graph Engine

생각 파편을 8개의 고정된 가치 축으로 매핑합니다.

**8개 Value Axes (고정):**
1. `GROWTH` - 성장/학습
2. `STABILITY` - 안정/예측가능
3. `FINANCIAL` - 금전/보상
4. `AUTONOMY` - 자율/통제
5. `RELATIONSHIP` - 관계/소속
6. `ACHIEVEMENT` - 성취/인정
7. `HEALTH` - 건강/에너지
8. `MEANING` - 의미/기여

**핵심 원칙:**
- 모순 허용: Conflict edge는 명시적 제거 금지
- 다중 귀속: 하나의 파편이 여러 가치에 매핑 가능 (합 ≠ 1)
- 시간 변화: Weight는 시간에 따라 변화 허용

```kotlin
data class ValueNode(
    val axis: ValueAxis,
    val avgValence: Double,
    val recentTrend: Trend,
    val fragmentCount: Double // sum of weights (effective count)
)

data class ValueEdge(
    val fromValue: ValueAxis,
    val toValue: ValueAxis,
    val edgeType: EdgeType,        // SUPPORT or CONFLICT
    val weight: Double,
    val lastUpdated: Instant
)
```

### 4.3 Decision Projection Engine

과거의 파편을 기반으로 결정의 적합도와 후회 위험을 계산합니다.

**입력:**
- `decisionTitle`: 결정 제목
- `optionA` / `optionB`: 두 가지 선택지 (A/B만 지원)
- `optionalPriority`: 우선시할 가치 축 (선택)

**처리 흐름:**
1. **유사 파편 검색**: Decision context → embedding → topK fragments
2. **가치 적합도 계산**: Option → expected value vector → cosine similarity
3. **후회 위험 추정**: Historical regret rate + loss aversion weight (λ)
4. **확률화**: Softmax(scoreA, scoreB)

```kotlin
data class DecisionResult(
    val probabilityA: Double,      // P(A|Me)
    val probabilityB: Double,      // P(B|Me)
    val regretRiskA: Double,
    val regretRiskB: Double,
    val evidenceFragments: List<FragmentId>,  // Top 5
    val valueAlignment: Map<ValueAxis, Double>
)
```

**계산 공식:**
```
scoreA = fitA - λ * regretA
scoreB = fitB - λ * regretB
P(A|Me) = exp(scoreA) / (exp(scoreA) + exp(scoreB))
```

### 4.4 LLM Explanation Layer

계산 결과를 자연어로 설명하는 계층입니다.

**⚠️ 엄격한 제한:**
- ❌ 추천/조언 생성 금지
- ❌ "A를 선택하세요" 류의 판단 금지
- ✅ "왜 이런 결과가 나왔는지" 설명만 허용

**MCP Integration:**
```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        transport: stdio
```

## 5. Data Flow

### 5.1 Fragment Input Flow
```
User Input → API → Emotion Analysis → Embedding → Store
                        ↓
                   moodValence
                   arousal
```

### 5.2 Value Graph Update Flow
```
New Fragment → Embedding → Value Axis Similarity → Soft Assignment
                                    ↓
                          Update ValueNode stats
                          Update ValueEdge weights
```

### 5.3 Decision Projection Flow
```
Decision Request → Context Embedding → Similar Fragments (TopK)
                                              ↓
                                    Value Fit Calculation
                                              ↓
                                    Regret Risk Estimation
                                              ↓
                                    Softmax Probability
                                              ↓
                                    LLM Explanation (설명만)
                                              ↓
                                    Response to User
```

## 6. Database Schema

### 6.1 Tables Overview

```sql
-- Append-only fragment storage
CREATE TABLE thought_fragments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    text_raw TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    mood_valence DOUBLE PRECISION NOT NULL,
    arousal DOUBLE PRECISION NOT NULL,
    topic_hint VARCHAR(255),
    embedding vector(1536),
    deleted_at TIMESTAMPTZ,

    -- Append-only: no UPDATE allowed via application
    CONSTRAINT chk_valence CHECK (mood_valence BETWEEN -1.0 AND 1.0),
    CONSTRAINT chk_arousal CHECK (arousal BETWEEN 0.0 AND 1.0)
);

-- Value graph nodes
CREATE TABLE value_nodes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    axis VARCHAR(50) NOT NULL,
    avg_valence DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    recent_trend VARCHAR(20) NOT NULL DEFAULT 'NEUTRAL',
    fragment_count DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(user_id, axis)
);

-- Value graph edges (모순 허용)
CREATE TABLE value_edges (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    from_axis VARCHAR(50) NOT NULL,
    to_axis VARCHAR(50) NOT NULL,
    edge_type VARCHAR(20) NOT NULL,  -- SUPPORT or CONFLICT
    weight DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(user_id, from_axis, to_axis)
);

-- Decision records
CREATE TABLE decisions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    priority_axis VARCHAR(50),
    probability_a DOUBLE PRECISION NOT NULL,
    probability_b DOUBLE PRECISION NOT NULL,
    regret_risk_a DOUBLE PRECISION NOT NULL,
    regret_risk_b DOUBLE PRECISION NOT NULL,
    evidence_fragment_ids UUID[] NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Feedback for learning
CREATE TABLE decision_feedbacks (
    id UUID PRIMARY KEY,
    decision_id UUID NOT NULL REFERENCES decisions(id),
    feedback_type VARCHAR(20) NOT NULL,  -- SATISFIED, NEUTRAL, REGRET
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 6.2 Indexes

```sql
-- Vector similarity search
CREATE INDEX idx_fragments_embedding ON thought_fragments
    USING hnsw (embedding vector_cosine_ops);

-- Time-based queries
CREATE INDEX idx_fragments_user_created ON thought_fragments(user_id, created_at DESC);
CREATE INDEX idx_decisions_user_created ON decisions(user_id, created_at DESC);
```

## 7. API Design

### 7.1 Fragment API

```
POST   /api/v1/fragments              # Create fragment
GET    /api/v1/fragments              # List fragments (paginated)
GET    /api/v1/fragments/{id}         # Get fragment
DELETE /api/v1/fragments/{id}         # Soft-delete fragment
```

### 7.2 Decision API

```
POST   /api/v1/decisions              # Create decision projection
GET    /api/v1/decisions              # List past decisions
GET    /api/v1/decisions/{id}         # Get decision detail
POST   /api/v1/decisions/{id}/feedback # Submit feedback
```

### 7.3 Value Graph API (Read-only)

```
GET    /api/v1/values                 # Get user's value graph
GET    /api/v1/values/{axis}          # Get specific axis detail
```

## 8. Security Considerations

### 8.1 MVP Phase
- Single user assumption OR simple token auth
- All data is personal and sensitive

### 8.2 Future Considerations
- End-to-end encryption for thought fragments
- Data export/deletion (GDPR compliance)
- Audit logging for all operations

## 9. Performance Targets

| Operation | Target Latency |
|-----------|---------------|
| Fragment input → storage | < 1 second |
| Similar fragment search (topK) | < 500ms |
| Decision projection | < 3 seconds |
| LLM explanation generation | < 5 seconds |

## 10. Development Environment

### Docker Compose Setup
```yaml
services:
  postgres:
    image: pgvector/pgvector:pg17
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: pros
      POSTGRES_USER: pros
      POSTGRES_PASSWORD: pros_dev
    volumes:
      - postgres_data:/var/lib/postgresql/data
```

## 11. References

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring AI MCP Overview](https://docs.spring.io/spring-ai/reference/1.1-SNAPSHOT/api/mcp/mcp-overview.html)
- [pgvector GitHub](https://github.com/pgvector/pgvector)
- [Model Context Protocol](https://modelcontextprotocol.io/)
