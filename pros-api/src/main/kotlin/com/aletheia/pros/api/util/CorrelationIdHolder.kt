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
