# Design: Error Tracking 개선

> 백엔드 에러 로깅 및 추적 시스템 상세 설계

**Plan 문서**: `docs/01-plan/features/error-tracking.plan.md`

---

## 1. 아키텍처 설계

### 1.1 컴포넌트 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Request Flow                                   │
└─────────────────────────────────────────────────────────────────────────┘

  Client Request
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  RequestLoggingFilter (Order: Ordered.HIGHEST_PRECEDENCE)                │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │ 1. X-Correlation-ID 헤더 확인 (있으면 사용, 없으면 생성)          │  │
│  │ 2. MDC.put("correlationId", id)                                    │  │
│  │ 3. MDC.put("requestMethod", method)                                │  │
│  │ 4. MDC.put("requestUri", uri)                                      │  │
│  │ 5. 요청 시작 시간 기록                                             │  │
│  │ 6. Request 로깅 (DEBUG 레벨)                                       │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  JwtAuthenticationFilter (기존)                                          │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  Controller → Service → Repository                                       │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼ (예외 발생 시)
┌──────────────────────────────────────────────────────────────────────────┐
│  GlobalExceptionHandler (Enhanced)                                       │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │ 1. 예외 유형별 로깅 레벨 적용                                      │  │
│  │    - 4xx: WARN (클라이언트 오류)                                   │  │
│  │    - 5xx: ERROR (서버 오류, 스택트레이스 포함)                     │  │
│  │ 2. MDC에서 correlationId 추출                                      │  │
│  │ 3. ErrorResponse에 correlationId 포함                              │  │
│  │ 4. 응답 헤더에 X-Correlation-ID 추가                               │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  RequestLoggingFilter (afterCompletion)                                  │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │ 1. Response 상태 코드 로깅                                         │  │
│  │ 2. 처리 시간 계산 및 로깅                                          │  │
│  │ 3. MDC.clear()                                                     │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
```

### 1.2 패키지 구조

```
pros-api/src/main/kotlin/com/aletheia/pros/api/
├── config/
│   └── WebConfig.kt                    # (기존) + Filter 등록
├── filter/
│   └── RequestLoggingFilter.kt         # NEW: 요청/응답 로깅
├── exception/
│   └── GlobalExceptionHandler.kt       # MODIFY: 상세 로깅 추가
├── dto/response/
│   └── ErrorResponse.kt                # MODIFY: correlationId 추가
└── util/
    └── CorrelationIdHolder.kt          # NEW: MDC 유틸리티

pros-bootstrap/src/main/resources/
├── logback-spring.xml                  # NEW: 로깅 포맷 설정
└── application.yml                     # MODIFY: 로깅 레벨 조정
```

---

## 2. 상세 설계

### 2.1 CorrelationIdHolder

**파일**: `pros-api/src/main/kotlin/com/aletheia/pros/api/util/CorrelationIdHolder.kt`

```kotlin
package com.aletheia.pros.api.util

import org.slf4j.MDC
import java.util.UUID

/**
 * MDC 기반 Correlation ID 관리 유틸리티.
 * 요청 추적을 위한 고유 ID를 관리한다.
 */
object CorrelationIdHolder {

    const val CORRELATION_ID_KEY = "correlationId"
    const val CORRELATION_ID_HEADER = "X-Correlation-ID"

    /**
     * 새로운 Correlation ID를 생성하고 MDC에 설정한다.
     */
    fun generate(): String {
        val id = UUID.randomUUID().toString().substring(0, 8)
        MDC.put(CORRELATION_ID_KEY, id)
        return id
    }

    /**
     * 기존 Correlation ID를 MDC에 설정한다.
     */
    fun set(id: String) {
        MDC.put(CORRELATION_ID_KEY, id)
    }

    /**
     * 현재 Correlation ID를 반환한다.
     */
    fun get(): String? = MDC.get(CORRELATION_ID_KEY)

    /**
     * MDC를 정리한다.
     */
    fun clear() {
        MDC.clear()
    }
}
```

**설계 결정**:
- UUID의 앞 8자리만 사용하여 로그 가독성 확보
- `object` 싱글톤으로 정적 유틸리티 제공
- MDC(Mapped Diagnostic Context) 사용으로 스레드 안전성 보장

---

### 2.2 RequestLoggingFilter

**파일**: `pros-api/src/main/kotlin/com/aletheia/pros/api/filter/RequestLoggingFilter.kt`

```kotlin
package com.aletheia.pros.api.filter

import com.aletheia.pros.api.util.CorrelationIdHolder
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private val logger = KotlinLogging.logger {}

/**
 * 요청/응답 로깅 필터.
 * 모든 요청에 Correlation ID를 부여하고 로깅한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val startTime = System.currentTimeMillis()

        // 1. Correlation ID 설정 (헤더에서 가져오거나 새로 생성)
        val correlationId = request.getHeader(CorrelationIdHolder.CORRELATION_ID_HEADER)
            ?: CorrelationIdHolder.generate()

        if (request.getHeader(CorrelationIdHolder.CORRELATION_ID_HEADER) != null) {
            CorrelationIdHolder.set(correlationId)
        }

        // 2. 응답 헤더에 Correlation ID 추가
        response.setHeader(CorrelationIdHolder.CORRELATION_ID_HEADER, correlationId)

        // 3. 요청 로깅
        logger.debug {
            "→ ${request.method} ${request.requestURI}${request.queryString?.let { "?$it" } ?: ""}"
        }

        try {
            filterChain.doFilter(request, response)
        } finally {
            // 4. 응답 로깅
            val duration = System.currentTimeMillis() - startTime
            val logMessage = "← ${response.status} ${request.method} ${request.requestURI} (${duration}ms)"

            when {
                response.status >= 500 -> logger.error { logMessage }
                response.status >= 400 -> logger.warn { logMessage }
                else -> logger.debug { logMessage }
            }

            // 5. MDC 정리
            CorrelationIdHolder.clear()
        }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // 헬스체크, 정적 리소스 등 제외
        val path = request.requestURI
        return path.startsWith("/actuator/health") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs")
    }
}
```

**설계 결정**:
- `OncePerRequestFilter` 상속으로 요청당 1회 실행 보장
- `Ordered.HIGHEST_PRECEDENCE`로 가장 먼저 실행
- 헬스체크 등 노이즈 요청은 필터링
- 응답 상태 코드에 따른 로그 레벨 자동 조정

---

### 2.3 ErrorResponse 개선

**파일**: `pros-api/src/main/kotlin/com/aletheia/pros/api/dto/response/ErrorResponse.kt`

```kotlin
package com.aletheia.pros.api.dto.response

import com.aletheia.pros.api.util.CorrelationIdHolder
import java.time.Instant

/**
 * Standard error response with correlation ID for request tracing.
 */
data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val correlationId: String? = CorrelationIdHolder.get()
)

/**
 * Validation error response with field-level errors.
 */
data class ValidationErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int = 400,
    val error: String = "Bad Request",
    val message: String = "Validation failed",
    val path: String,
    val errors: List<FieldError>,
    val correlationId: String? = CorrelationIdHolder.get()
)

/**
 * Individual field validation error.
 */
data class FieldError(
    val field: String,
    val message: String
)
```

**변경 사항**:
- `correlationId` 필드 추가 (nullable, MDC에서 자동 추출)

---

### 2.4 GlobalExceptionHandler 개선

**파일**: `pros-api/src/main/kotlin/com/aletheia/pros/api/exception/GlobalExceptionHandler.kt`

```kotlin
package com.aletheia.pros.api.exception

import com.aletheia.pros.api.dto.response.ErrorResponse
import com.aletheia.pros.api.dto.response.FieldError
import com.aletheia.pros.api.dto.response.ValidationErrorResponse
import com.aletheia.pros.api.util.CorrelationIdHolder
import com.aletheia.pros.application.exception.EmbeddingGenerationException
import com.aletheia.pros.application.exception.QuotaExceededException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val logger = KotlinLogging.logger {}

/**
 * Global exception handler for REST API.
 * 모든 예외를 캐치하여 일관된 에러 응답과 로깅을 제공한다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    // ===== 4xx Client Errors (WARN level) =====

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ValidationErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { error ->
            FieldError(
                field = error.field,
                message = error.defaultMessage ?: "Invalid value"
            )
        }

        logger.warn { "Validation failed: ${errors.map { "${it.field}: ${it.message}" }}" }

        val response = ValidationErrorResponse(
            path = request.requestURI,
            errors = errors
        )

        return ResponseEntity.badRequest().body(response)
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(
        ex: MissingRequestHeaderException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "Missing required header: ${ex.headerName}" }

        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = "Missing required header: ${ex.headerName}",
            path = request.requestURI
        )

        return ResponseEntity.badRequest().body(response)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedJson(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "Malformed JSON request: ${ex.message?.take(200)}" }

        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = "Malformed JSON request",
            path = request.requestURI
        )

        return ResponseEntity.badRequest().body(response)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn { "Illegal argument: ${ex.message}" }

        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "Invalid argument",
            path = request.requestURI
        )

        return ResponseEntity.badRequest().body(response)
    }

    // ===== 5xx Server Errors (ERROR level with stacktrace) =====

    @ExceptionHandler(QuotaExceededException::class)
    fun handleQuotaExceeded(
        ex: QuotaExceededException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error(ex) { "OpenAI API quota exceeded" }

        val response = ErrorResponse(
            status = HttpStatus.PAYMENT_REQUIRED.value(),
            error = "Payment Required",
            message = ex.message ?: "OpenAI API quota exceeded. Please check your plan and billing details.",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response)
    }

    @ExceptionHandler(EmbeddingGenerationException::class)
    fun handleEmbeddingGenerationException(
        ex: EmbeddingGenerationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error(ex) { "Embedding generation failed: ${ex.message}" }

        val response = ErrorResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE.value(),
            error = "Service Unavailable",
            message = ex.message ?: "Failed to generate embedding. Please try again later.",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        // 상세 스택트레이스 로깅 (correlationId가 자동으로 포함됨)
        logger.error(ex) {
            "Unexpected error occurred [${ex.javaClass.simpleName}]: ${ex.message}"
        }

        val response = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred. Please contact support with correlationId: ${CorrelationIdHolder.get()}",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}
```

**개선 사항**:
- 4xx 에러: WARN 레벨로 로깅 (스택트레이스 없음)
- 5xx 에러: ERROR 레벨로 로깅 (스택트레이스 포함)
- 모든 핸들러에 로깅 추가
- 에러 메시지에 correlationId 안내 포함

---

### 2.5 Logback 설정

**파일**: `pros-bootstrap/src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- 프로퍼티 정의 -->
    <property name="LOG_PATTERN"
              value="%d{HH:mm:ss.SSS} %highlight(%-5level) [%X{correlationId:---------}] %cyan(%-40.40logger{39}) : %msg%n"/>

    <!-- 콘솔 Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 프로파일별 설정 -->
    <springProfile name="local,dev">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>

        <!-- 애플리케이션 로그 -->
        <logger name="com.aletheia.pros" level="DEBUG"/>

        <!-- 요청 로깅 -->
        <logger name="com.aletheia.pros.api.filter" level="DEBUG"/>

        <!-- SQL 로깅 (필요시) -->
        <logger name="org.hibernate.SQL" level="DEBUG"/>
        <logger name="org.hibernate.type.descriptor.sql" level="TRACE"/>
    </springProfile>

    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>

        <logger name="com.aletheia.pros" level="INFO"/>
        <logger name="com.aletheia.pros.api.filter" level="INFO"/>
    </springProfile>

</configuration>
```

**로그 출력 예시**:
```
14:32:15.123 DEBUG [a1b2c3d4] c.a.p.api.filter.RequestLoggingFilter    : → POST /v1/fragments
14:32:15.456 ERROR [a1b2c3d4] c.a.p.api.exception.GlobalExceptionHandler : Unexpected error [NullPointerException]: ...
java.lang.NullPointerException: null
    at com.aletheia.pros.application.usecase...
    ...
14:32:15.789 ERROR [a1b2c3d4] c.a.p.api.filter.RequestLoggingFilter    : ← 500 POST /v1/fragments (666ms)
```

---

## 3. 구현 순서

| 순서 | 파일 | 작업 |
|------|------|------|
| 1 | `CorrelationIdHolder.kt` | 신규 생성 |
| 2 | `RequestLoggingFilter.kt` | 신규 생성 |
| 3 | `ErrorResponse.kt` | correlationId 필드 추가 |
| 4 | `GlobalExceptionHandler.kt` | 로깅 개선 |
| 5 | `logback-spring.xml` | 신규 생성 |
| 6 | `application-dev.yml` | 로깅 설정 정리 |

---

## 4. 테스트 계획

### 4.1 수동 테스트

```bash
# 1. 정상 요청 테스트
curl -v -X POST http://localhost:8080/api/v1/fragments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"text": "test"}'
# 확인: 응답 헤더에 X-Correlation-ID 포함

# 2. 에러 요청 테스트 (잘못된 JSON)
curl -v -X POST http://localhost:8080/api/v1/fragments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{invalid json}'
# 확인: 서버 로그에 correlationId와 함께 WARN 로그 출력

# 3. 서버 에러 테스트
# 확인: 서버 로그에 correlationId와 스택트레이스 출력
```

### 4.2 검증 항목

- [ ] 모든 요청에 Correlation ID 생성됨
- [ ] 응답 헤더에 X-Correlation-ID 포함됨
- [ ] 에러 응답 JSON에 correlationId 포함됨
- [ ] 4xx 에러는 WARN 레벨로 로깅됨
- [ ] 5xx 에러는 ERROR 레벨 + 스택트레이스로 로깅됨
- [ ] 로그에 correlationId가 표시됨

---

## 5. 호환성

- **하위 호환성**: 기존 API 응답 구조에 `correlationId` 필드만 추가 (optional)
- **성능 영향**: 무시할 수준 (UUID 생성 + MDC 설정)
- **의존성**: 추가 라이브러리 없음 (SLF4J MDC 사용)

---

**작성일**: 2026-01-31
**상태**: Ready for Implementation
**다음 단계**: 구현 시작 → `/pdca do error-tracking`
