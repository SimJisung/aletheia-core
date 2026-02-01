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

    /**
     * Handles validation errors from @Valid annotations.
     */
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

    /**
     * Handles missing required headers.
     */
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

    /**
     * Handles malformed JSON requests.
     */
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

    /**
     * Handles illegal argument exceptions.
     */
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

    /**
     * Handles OpenAI API quota exceeded errors.
     */
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

    /**
     * Handles embedding generation failures.
     */
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

    /**
     * Handles all other exceptions.
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        // 상세 스택트레이스 로깅 (correlationId가 자동으로 포함됨)
        logger.error(ex) {
            "Unexpected error occurred [${ex.javaClass.simpleName}]: ${ex.message}"
        }

        val correlationId = CorrelationIdHolder.get()
        val response = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred. Please contact support with correlationId: $correlationId",
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}
