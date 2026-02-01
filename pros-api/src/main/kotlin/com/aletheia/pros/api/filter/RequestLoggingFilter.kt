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
            ?.also { CorrelationIdHolder.set(it) }
            ?: CorrelationIdHolder.generate()

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
