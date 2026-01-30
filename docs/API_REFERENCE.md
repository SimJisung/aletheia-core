# PROS API Reference

Base URL: `http://localhost:8080/api`

All endpoints require the `X-User-Id` header for user identification.

---

## Authentication

All requests must include the `X-User-Id` header:

```http
X-User-Id: <uuid>
```

---

## Fragments API

Fragments are immutable thought records. They can only be created or soft-deleted.

### Create Fragment

Creates a new thought fragment with automatic emotion analysis and embedding generation.

```http
POST /v1/fragments
```

**Request Headers:**
| Header | Type | Required | Description |
|--------|------|----------|-------------|
| X-User-Id | UUID | Yes | User identifier |
| Content-Type | string | Yes | application/json |

**Request Body:**
```json
{
  "text": "오늘 정말 기분 좋은 하루였다",
  "topicHint": "daily"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| text | string | Yes | Fragment text (max 10,000 characters) |
| topicHint | string | No | Optional topic categorization hint |

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "text": "오늘 정말 기분 좋은 하루였다",
  "moodValence": 0.8,
  "arousal": 0.6,
  "topicHint": "daily",
  "createdAt": "2026-01-30T10:30:00Z",
  "isDeleted": false
}
```

**Error Responses:**
- `400 Bad Request` - Invalid input (empty text, text too long)

---

### Get Fragment

Retrieves a fragment by ID.

```http
GET /v1/fragments/{id}
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| id | UUID | Fragment ID |

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "text": "오늘 정말 기분 좋은 하루였다",
  "moodValence": 0.8,
  "arousal": 0.6,
  "topicHint": "daily",
  "createdAt": "2026-01-30T10:30:00Z",
  "isDeleted": false
}
```

**Error Responses:**
- `404 Not Found` - Fragment not found or not owned by user

---

### List Fragments

Lists fragments with pagination.

```http
GET /v1/fragments?limit=20&offset=0
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| limit | int | 20 | Max results (1-100) |
| offset | int | 0 | Skip N results |

**Response:** `200 OK`
```json
{
  "fragments": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "text": "오늘 정말 기분 좋은 하루였다",
      "moodValence": 0.8,
      "arousal": 0.6,
      "createdAt": "2026-01-30T10:30:00Z"
    }
  ],
  "total": 150,
  "hasMore": true
}
```

---

### Delete Fragment (Soft Delete)

Marks a fragment as deleted. The data is preserved (append-only principle).

```http
DELETE /v1/fragments/{id}
```

**Response:** `204 No Content`

**Error Responses:**
- `404 Not Found` - Fragment not found or not owned by user

---

### Find Similar Fragments

Finds semantically similar fragments using vector search.

```http
GET /v1/fragments/similar?queryText=career&topK=10
```

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| queryText | string | - | Search query text |
| topK | int | 10 | Max results (1-50) |

**Response:** `200 OK`
```json
[
  {
    "fragment": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "text": "새로운 커리어에 대해 고민중...",
      "moodValence": 0.3,
      "arousal": 0.5
    },
    "similarity": 0.89
  }
]
```

---

## Decisions API

Decisions are binary choice projections based on user's historical patterns.

**IMPORTANT:** Decisions provide probabilities, NOT recommendations. The user makes the final choice.

### Create Decision

Creates a decision projection analyzing how each option fits user patterns.

```http
POST /v1/decisions
```

**Request Body:**
```json
{
  "title": "이직 결정",
  "optionA": "현재 회사에 남기",
  "optionB": "새 회사로 이직하기",
  "priorityAxis": "FINANCIAL"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| title | string | Yes | Decision title (max 500 chars) |
| optionA | string | Yes | First option (max 2000 chars) |
| optionB | string | Yes | Second option (max 2000 chars) |
| priorityAxis | string | No | Priority value axis (see Value Axes) |

**Response:** `201 Created`
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "title": "이직 결정",
  "optionA": "현재 회사에 남기",
  "optionB": "새 회사로 이직하기",
  "priorityAxis": "FINANCIAL",
  "result": {
    "probabilityA": 45,
    "probabilityB": 55,
    "regretRiskA": 30,
    "regretRiskB": 25,
    "evidenceCount": 5
  },
  "createdAt": "2026-01-30T10:30:00Z"
}
```

---

### Get Decision

Retrieves a decision by ID.

```http
GET /v1/decisions/{id}
```

**Response:** `200 OK` (same format as Create Decision response)

---

### Get Decision Explanation

Gets LLM-generated explanation for why the decision projection produced these results.

```http
GET /v1/decisions/{id}/explanation
```

**Response:** `200 OK`
```json
{
  "summary": "과거 기록에 따르면 B 선택지가 약간 더 높은 적합도를 보입니다.",
  "evidenceSummary": "최근 3개월간의 생각 파편에서 변화에 대한 긍정적 기록이 발견되었습니다.",
  "valueSummary": "이 결정은 성장과 재정적 안정 가치 사이의 선택을 반영합니다."
}
```

**Note:** The explanation describes calculations, NOT recommendations.

---

### List Decisions

Lists decisions with pagination.

```http
GET /v1/decisions?limit=20&offset=0
```

**Response:** `200 OK`
```json
{
  "decisions": [...],
  "total": 25,
  "hasMore": true
}
```

---

### Submit Feedback

Submits feedback on a decision to improve future projections.

```http
POST /v1/decisions/{id}/feedback
```

**Request Body:**
```json
{
  "feedbackType": "CHOSE_A"
}
```

| feedbackType | Description |
|--------------|-------------|
| CHOSE_A | User chose option A |
| CHOSE_B | User chose option B |
| POSTPONED | User postponed the decision |
| REGRET_A | User regrets choosing A |
| REGRET_B | User regrets choosing B |
| SATISFIED | User is satisfied with their choice |

**Response:** `201 Created`
```json
{
  "id": "770e8400-e29b-41d4-a716-446655440002",
  "decisionId": "660e8400-e29b-41d4-a716-446655440001",
  "feedbackType": "CHOSE_A",
  "submittedAt": "2026-01-31T10:30:00Z"
}
```

**Error Responses:**
- `404 Not Found` - Decision not found
- `409 Conflict` - Feedback already submitted for this decision

---

### Get Pending Feedback

Gets decisions awaiting feedback (24-72 hours old without feedback).

```http
GET /v1/decisions/pending-feedback
```

**Response:** `200 OK`
```json
[
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "title": "이직 결정",
    "createdAt": "2026-01-28T10:30:00Z"
  }
]
```

---

## Values API

The Value Graph represents user's 8 value dimensions based on accumulated fragments.

### Value Axes

| Axis | Korean | Description |
|------|--------|-------------|
| GROWTH | 성장/학습 | Personal development, learning |
| STABILITY | 안정/예측가능 | Security, predictability |
| FINANCIAL | 금전/보상 | Money, economic security |
| AUTONOMY | 자율/통제 | Independence, freedom |
| RELATIONSHIP | 관계/소속 | Social connections, belonging |
| ACHIEVEMENT | 성취/인정 | Success, accomplishment |
| HEALTH | 건강/에너지 | Physical/mental wellbeing |
| MEANING | 의미/기여 | Purpose, significance |

### Get Value Graph

Gets the user's complete value graph with all nodes and edges.

```http
GET /v1/values
```

**Response:** `200 OK`
```json
{
  "nodes": [
    {
      "axis": "GROWTH",
      "displayName": "성장/학습",
      "avgValence": 0.65,
      "recentTrend": "RISING",
      "fragmentCount": 12
    }
  ],
  "edges": [
    {
      "fromAxis": "GROWTH",
      "toAxis": "STABILITY",
      "edgeType": "CONFLICT",
      "weight": 0.4
    }
  ],
  "conflictCount": 3
}
```

---

### Get Value Axis

Gets a specific value axis for the user.

```http
GET /v1/values/{axis}
```

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| axis | string | Value axis name (e.g., GROWTH) |

**Response:** `200 OK`
```json
{
  "axis": "GROWTH",
  "displayName": "성장/학습",
  "avgValence": 0.65,
  "recentTrend": "RISING",
  "fragmentCount": 12
}
```

---

### Get All Value Axes

Gets definitions for all 8 value axes.

```http
GET /v1/values/axes
```

**Response:** `200 OK`
```json
[
  {
    "name": "GROWTH",
    "displayNameKo": "성장/학습",
    "displayNameEn": "Growth/Learning",
    "description": "Personal development, learning, self-improvement"
  }
]
```

---

### Get Value Edges

Gets all edges in the user's value graph.

```http
GET /v1/values/edges
```

**Response:** `200 OK`
```json
[
  {
    "fromAxis": "GROWTH",
    "toAxis": "STABILITY",
    "edgeType": "CONFLICT",
    "weight": 0.4
  }
]
```

Edge types:
- `SUPPORT` - Values reinforce each other
- `CONFLICT` - Values are in tension (this is normal!)

---

### Get Value Conflicts

Gets value conflicts (tensions between values).

**Note:** Conflicts are NORMAL. Humans naturally have competing values.

```http
GET /v1/values/conflicts
```

**Response:** `200 OK`
```json
[
  {
    "axis1": "GROWTH",
    "axis2": "STABILITY",
    "tensionLevel": 0.6,
    "description": "성장 추구와 안정 선호 사이의 긴장"
  }
]
```

---

### Get Value Summary

Gets a summary of the user's value profile.

```http
GET /v1/values/summary
```

**Response:** `200 OK`
```json
{
  "topPositiveValues": ["GROWTH", "RELATIONSHIP"],
  "topNegativeValues": ["FINANCIAL"],
  "dominantTrend": "RISING",
  "conflictCount": 3,
  "totalFragments": 45
}
```

---

## Error Responses

All endpoints may return these error responses:

| Status | Description |
|--------|-------------|
| 400 Bad Request | Invalid input data |
| 404 Not Found | Resource not found or not owned by user |
| 409 Conflict | Resource already exists (e.g., duplicate feedback) |
| 500 Internal Server Error | Server error |

Error response format:
```json
{
  "timestamp": "2026-01-30T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Fragment text cannot be blank",
  "path": "/api/v1/fragments"
}
```

---

## Rate Limiting

Currently no rate limiting is applied. This may change in production.

---

## Versioning

The API is versioned via URL path (`/v1/`). Breaking changes will result in a new version.
