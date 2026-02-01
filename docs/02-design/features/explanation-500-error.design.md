# Design: Decision Explanation 500 Error Fix

## Problem Statement
`GET /api/v1/decisions/{id}/explanation` 호출 시 500 에러가 발생하지만 에러 로그가 기록되지 않는 문제

## Root Cause Analysis

### 1. Blocking Call in Coroutine Context
`LlmExplanationAdapter`의 `ChatClient.call()`은 **blocking call**입니다.
Spring MVC suspend function에서 blocking call을 직접 호출하면:
- 코루틴 스레드가 블로킹됨
- 예외 전파가 제대로 되지 않을 수 있음
- 타임아웃 발생 시 예외 정보가 손실될 수 있음

### 2. Missing Exception Logging
기존 코드에서 try-catch가 있었지만 `warn` 레벨 로깅만 수행하고 있어 운영 환경에서 에러가 눈에 띄지 않음

## Solution Design

### 수정 1: Blocking Call 분리
```kotlin
// Before
val response = chatClient.prompt(prompt).call().content() ?: ""

// After
val response = withContext(Dispatchers.IO) {
    chatClient.prompt(prompt).call().content() ?: ""
}
```

**이유:**
- `Dispatchers.IO`는 blocking I/O 작업을 위한 스레드 풀 사용
- 코루틴 메인 스레드가 블로킹되지 않음
- 예외가 적절히 코루틴 컨텍스트로 전파됨

### 수정 2: 로깅 강화
```kotlin
// 호출 전 로그
logger.debug { "Calling ChatClient for explanation generation..." }

// 예외 발생 시 상세 로그
logger.error(e) { "ChatClient call failed: ${e.javaClass.simpleName} - ${e.message}" }

// 성공 시 로그
logger.debug { "ChatClient response received, length: ${response.length}" }
```

**이유:**
- 에러 발생 위치 정확히 파악
- 예외 타입과 메시지 명확히 기록
- 스택트레이스 포함 (`logger.error(e)`)

### 수정 3: 의존성 추가
```kotlin
// pros-infrastructure/build.gradle.kts
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
```

## Modified Files

| File | Changes |
|------|---------|
| `pros-infrastructure/.../LlmExplanationAdapter.kt` | withContext, 로깅 강화 |
| `pros-infrastructure/build.gradle.kts` | kotlinx-coroutines 의존성 추가 |

## Expected Behavior After Fix

### Case 1: ChatClient 정상 동작
- DEBUG 로그: `Calling ChatClient for explanation generation...`
- DEBUG 로그: `ChatClient response received, length: XXX`
- 정상 응답 반환

### Case 2: ChatClient 예외 발생
- ERROR 로그: `ChatClient call failed: ExceptionType - message` (스택트레이스 포함)
- ERROR 로그: `Failed to generate explanation for decision XXX: ...`
- **Fallback 응답 반환** (500 에러 대신 기본 explanation)

### Case 3: 타임아웃
- ERROR 로그: `ChatClient call failed: SocketTimeoutException - ...`
- Fallback 응답 반환

## Test Plan

1. 애플리케이션 재시작
2. `GET /api/v1/decisions/{id}/explanation` 호출
3. 로그 확인:
   - 정상: DEBUG 로그로 ChatClient 호출/응답 확인
   - 에러: ERROR 로그로 상세 예외 정보 확인
4. 응답 확인:
   - 정상: LLM 생성 explanation
   - 에러: 기본 fallback explanation (500이 아님)

## Potential Follow-up Issues

### 1. Ollama 연결 문제 (local 프로필)
만약 에러 로그에 연결 실패가 표시되면:
- Ollama 서버 실행 여부 확인
- `OLLAMA_BASE_URL` 환경 변수 확인

### 2. OpenAI API 문제 (dev/prod 프로필)
- `OPENAI_API_KEY` 확인
- API 할당량 확인

### 3. 타임아웃 조정 필요 시
```yaml
spring:
  ai:
    openai:
      chat:
        options:
          timeout: 30000  # ms
```
