package com.example.chatbot.controller;

import com.example.chatbot.domain.ErrorResponse;
import com.example.chatbot.service.QuotaExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 실패가 그대로 500 으로 나가면 사용자도 우리도 원인을 알 수 없다.
 * 다시 시도해서 될 실패와, 다시 시도해도 안 될 실패를 나눠서 알려 준다.
 *
 * 여기로 오는 것은 스트림이 "시작되기 전"에 터진 실패뿐이다.
 * 이미 흐르기 시작한 뒤의 실패는 HTTP 상태를 바꿀 수 없으므로
 * ChatController 안에서 error 이벤트로 내보낸다.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getDefaultMessage())
                .orElse("요청이 올바르지 않다");
        return ResponseEntity.badRequest().body(new ErrorResponse(msg));
    }

    // 사용량 초과 — 모델을 부르기 전에 막았다
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuota(QuotaExceededException e) {
        log.info("사용량 초과로 요청을 막았다: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse(e.getMessage()));
    }

    // 재시도를 다 쓰고도 실패한 일시적 오류 — Rate Limit, 모델 서버 오류
    @ExceptionHandler(TransientAiException.class)
    public ResponseEntity<ErrorResponse> handleTransient(TransientAiException e) {
        log.warn("AI 일시적 오류: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("모델이 지금 불안정하다. 잠시 후 다시 시도한다"));
    }

    // 다시 시도해도 안 되는 오류 — 키가 틀렸거나 요청이 잘못된 경우
    @ExceptionHandler(NonTransientAiException.class)
    public ResponseEntity<ErrorResponse> handleNonTransient(NonTransientAiException e) {
        log.error("AI 오류, 설정 확인이 필요하다: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("모델 호출 설정을 확인한다. OPENAI_API_KEY 부터 본다"));
    }
}
