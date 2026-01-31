# PROS (Personal Reasoning OS)

> "이 프로젝트는 '결정을 대신하는 AI'가 아니라 '과거의 나를 호출해주는 시스템'을 만드는 일이다."

과거의 생각 파편을 근거로, 현재의 결정이 '나다운지'와 '후회 위험'을 확률로 제시하는 개인 반추 OS의 MVP 구현

**Current Version**: `0.1.0-SNAPSHOT`

## Key Features

- ✅ **JWT Authentication**: Secure user registration and login
- ✅ **Thought Fragment Management**: Append-only memory with semantic search
- ✅ **Value Graph Auto-Update**: Automatically updated based on user fragments
- ✅ **Decision Projection**: Calculate decision fit and regret probability
- ✅ **LLM-Powered Explanations**: Natural language explanations for decisions
- ✅ **Feedback Loop**: User feedback improves decision accuracy over time
- ✅ **Vector Similarity Search**: Find related thoughts using pgvector
- ✅ **OpenAPI Documentation**: Interactive API explorer

## Core Principles

### ❌ 하지 않을 것
- LLM으로 판단/추천 생성
- 목표 설정/습관 강요 UX
- 멀티 도메인(연애·투자·이직 동시 지원)
- 정교한 수학 모델부터 설계

### ✅ 반드시 지킬 것
- Append-only memory (불변 파편)
- Value Graph는 모순 허용
- Decision 결과는 "확률 + 근거"만
- 설명과 계산의 분리

## Architecture

Hexagonal Architecture (Clean Architecture) 기반

```
pros-core/
├── pros-domain/           # Pure Kotlin Domain Models
├── pros-application/      # Use Cases & Ports
├── pros-infrastructure/   # Adapters (JPA, LLM)
├── pros-api/              # REST API
└── pros-bootstrap/        # Spring Boot Application
```

자세한 아키텍처는 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)를 참조하세요.

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.3.0 |
| Framework | Spring Boot 3.5.8 |
| AI Integration | Spring AI 1.1.2 |
| Database | PostgreSQL 17 + pgvector |
| Authentication | JWT (JSON Web Tokens) |
| API Documentation | SpringDoc OpenAPI 3 |
| Build | Gradle (Kotlin DSL) |
| JDK | Java 21 |

## Getting Started

### Prerequisites

- JDK 21+
- Docker & Docker Compose
- OpenAI API Key

### Setup

1. Clone the repository
```bash
git clone https://github.com/your-org/aletheia-core.git
cd aletheia-core
```

2. Start the database
```bash
docker-compose up -d
```

3. Configure environment
```bash
cp .env.example .env
# Edit .env and add your OPENAI_API_KEY
```

4. Build and run
```bash
./gradlew bootRun
```

### API Endpoints

#### Authentication
| Endpoint | Description |
|----------|-------------|
| `POST /api/v1/auth/register` | Register a new user account |
| `POST /api/v1/auth/login` | Login and get JWT token |

#### Thought Fragments
| Endpoint | Description |
|----------|-------------|
| `POST /api/v1/fragments` | Create a thought fragment |
| `GET /api/v1/fragments` | List fragments (paginated) |
| `GET /api/v1/fragments/{id}` | Get fragment by ID |
| `DELETE /api/v1/fragments/{id}` | Soft-delete fragment |
| `GET /api/v1/fragments/similar` | Find similar fragments (semantic search) |

#### Decisions
| Endpoint | Description |
|----------|-------------|
| `POST /api/v1/decisions` | Create decision projection |
| `GET /api/v1/decisions` | List decisions |
| `GET /api/v1/decisions/{id}` | Get decision by ID |
| `GET /api/v1/decisions/{id}/explanation` | Get decision explanation |
| `POST /api/v1/decisions/{id}/feedback` | Submit decision feedback |
| `GET /api/v1/decisions/pending-feedback` | Get decisions pending feedback |

#### Value Graph
| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/values` | Get user's value graph |
| `GET /api/v1/values/{axis}` | Get specific value axis details |
| `GET /api/v1/values/axes` | Get all value axes |
| `GET /api/v1/values/edges` | Get value relationships |
| `GET /api/v1/values/conflicts` | Get value conflicts |
| `GET /api/v1/values/summary` | Get value graph summary |

**Authentication**: All endpoints except `/auth/register` and `/auth/login` require JWT authentication via `Authorization: Bearer <token>` header.

**API Documentation**: Interactive API documentation is available at `http://localhost:8080/api/swagger-ui.html` when running locally.

## Core Concepts

### Thought Fragment (생각 파편)
사용자가 특정 순간에 기록한 생각, 감정, 메모의 최소 단위. 불변(Immutable), Append-only.

### Value Axis (가치 축)
8개의 고정된 가치 차원:
- 성장/학습 (GROWTH)
- 안정/예측가능 (STABILITY)
- 금전/보상 (FINANCIAL)
- 자율/통제 (AUTONOMY)
- 관계/소속 (RELATIONSHIP)
- 성취/인정 (ACHIEVEMENT)
- 건강/에너지 (HEALTH)
- 의미/기여 (MEANING)

### Decision Projection (결정 투영)
과거의 파편을 기반으로 현재 결정의 적합도와 후회 위험을 계산. **추천이 아닌 확률**로 제시.

## Documentation

- [Architecture](docs/ARCHITECTURE.md) - 시스템 아키텍처
- [Decision Principles](docs/DECISION_PRINCIPLES.md) - 설계 원칙 (LLM 금지 영역)
- [Glossary](docs/GLOSSARY.md) - 도메인 용어 사전

## License

MIT License
