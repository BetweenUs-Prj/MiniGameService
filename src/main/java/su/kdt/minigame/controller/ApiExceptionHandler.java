package su.kdt.minigame.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 전역 예외 처리 - 모든 500을 4xx/409로 매핑하여 500 제거
 */
@Slf4j
@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException e) {
        log.warn("Entity not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("code", "ENTITY_NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<Map<String, Object>> handleNoSuchElement(NoSuchElementException e) {
        log.warn("Element not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("code", "NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("code", "INVALID_ARGUMENT", "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        log.warn("Invalid state: {}", e.getMessage());
        
        // 중복 제출인 경우 특별 처리
        if (e.getMessage() != null && e.getMessage().contains("duplicate")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                    "code", "DUPLICATE_SUBMISSION", 
                    "message", "이미 제출한 답변입니다.", 
                    "alreadyAnswered", true
                ));
        }
        
        // 라운드 종료인 경우 410 Gone
        if (e.getMessage() != null && e.getMessage().contains("closed")) {
            return ResponseEntity.status(HttpStatus.GONE)
                .body(Map.of("code", "ROUND_CLOSED", "message", "라운드가 종료되었습니다."));
        }
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("code", "INVALID_STATE", "message", e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMessage());
        
        // 유니크 제약 위반 (중복 제출)
        if (e.getMessage() != null && e.getMessage().contains("uq_round_user")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                    "code", "DUPLICATE_SUBMISSION",
                    "message", "이미 제출한 답변입니다.",
                    "alreadyAnswered", true
                ));
        }
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("code", "DATA_INTEGRITY_VIOLATION", "message", "데이터 무결성 위반입니다."));
    }

    /**
     * 마지막 방어선 - 예상하지 못한 예외를 500으로 반환하되 로깅
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Unexpected exception occurred: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("code", "INTERNAL_ERROR", "message", "서버 내부 오류가 발생했습니다."));
    }
}