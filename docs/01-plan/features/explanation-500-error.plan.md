# Plan: Decision Explanation 500 Error Fix

## Overview
`/api/v1/decisions/{id}/explanation` 엔드포인트에서 500 에러가 발생하지만 에러 로그가 기록되지 않는 문제를 해결합니다.

## Problem Analysis

### 현상
- API 호출: `GET /api/v1/decisions/bc41fc6f-9f43-4dd6-b2cb-3dddfd6bb25f/explanation`
- 결과: 500 Internal Server Error
- 로그: 에러 상세 정보 없음

### 로그 분석
```
01:29:31.644 DEBUG LlmExplanationAdapter: Generating explanation for decision: bc41fc6f...
01:30:02.365 DEBUG RequestLoggingFilter: (ASYNC dispatch - 약 30초 후)
```

### 추정 원인
1. **ChatClient 예외 미처리**: `LlmExplanationAdapter`에서 ChatClient 호출 시 예외가 발생했으나,
   suspend function 내에서 발생한 예외가 Spring MVC의 `GlobalExceptionHandler`에서 캐치되지 않음
2. **Ollama 연결 문제**: local 프로필에서 Ollama 서버 연결 실패 가능성
3. **타임아웃**: OpenAI/Ollama API 응답 타임아웃

## Root Cause Investigation

### Check 1: ChatClient 예외 처리
- `LlmExplanationAdapter.explainDecision()`에서 try-catch 블록이 있지만,
  `chatClient.prompt(prompt).call()`이 blocking call이므로 코루틴에서 문제 발생 가능

### Check 2: Spring AI 설정
- Spring AI 1.1.2에서 ChatClient.Builder의 기본 설정 확인 필요
- Ollama vs OpenAI 모델 선택 확인

### Check 3: Exception Handler 호환성
- `GlobalExceptionHandler`가 suspend function에서 발생하는 예외를 처리하는지 확인

## Solution Plan

### Step 1: 로깅 강화
- ChatClient 호출 전후 상세 로그 추가
- 예외 발생 시 스택트레이스 포함 로깅

### Step 2: 예외 처리 개선
- ChatClient 호출을 `withContext(Dispatchers.IO)`로 감싸서 blocking call 분리
- Controller 레벨에서 명시적 예외 처리 추가

### Step 3: 타임아웃 설정
- ChatClient에 명시적 타임아웃 설정

### Step 4: Fallback 응답
- API 호출 실패 시 기본 응답 반환하도록 개선

## Success Criteria
1. API 호출 시 500 에러 대신 정상 응답 또는 명확한 에러 메시지 반환
2. 에러 발생 시 로그에 상세 정보 기록
3. 적절한 타임아웃 내 응답

## Timeline
- Step 1-2: 즉시 구현
- Step 3-4: 검증 후 필요시 추가

## Related Files
- `pros-infrastructure/.../LlmExplanationAdapter.kt`
- `pros-api/.../DecisionController.kt`
- `pros-api/.../GlobalExceptionHandler.kt`
- `pros-application/.../GetDecisionExplanationUseCase.kt`
