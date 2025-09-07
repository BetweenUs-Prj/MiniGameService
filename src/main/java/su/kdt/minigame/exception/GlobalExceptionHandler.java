package su.kdt.minigame.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import su.kdt.minigame.dto.response.ErrorResponse;

import java.sql.SQLException;
import java.util.Map;
import java.util.NoSuchElementException;



@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * DataIntegrityViolationException -> 409 Conflict or 422 Unprocessable Entity
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
        String sqlState = "n/a";
        if (root instanceof SQLException sqe) {
            sqlState = sqe.getSQLState();
        }
        
        log.warn("[ANSWERS-INTEGRITY] path={}, msg={}, sqlState={}, cause={}", 
            req.getRequestURI(),
            root.getMessage(),
            sqlState,
            ex.getClass().getSimpleName());
            
        // 1062: duplicate -> 409, 1452/1048: FK/NOT NULL -> 422
        int statusCode = (root.getMessage() != null && root.getMessage().contains("Duplicate")) ? 409 : 422;
        String errorCode = statusCode == 409 ? "DUPLICATE_ANSWER" : "INVALID_ANSWER_DATA";
        
        ErrorResponse errorResponse = ErrorResponse.of(errorCode, root.getMessage());
        return ResponseEntity.status(statusCode).body(errorResponse);
    }

    /**
     * NoResourceFoundException -> 404 Not Found (Spring MVC static resource error)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest req) {
        log.warn("[ANSWERS-ROUTING] NoResourceFound: path={}, resource={}", 
            req.getRequestURI(), ex.getResourcePath());
        
        ErrorResponse errorResponse = ErrorResponse.of("ENDPOINT_NOT_FOUND", 
            "The requested endpoint does not exist: " + ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * InvalidOptionException -> 422 Unprocessable Entity
     */
    @ExceptionHandler(InvalidOptionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOption(InvalidOptionException ex, HttpServletRequest req) {
        log.warn("[ANSWERS-INVALID-OPTION] path={}, roundId={}, optionId={}", 
            req.getRequestURI(), ex.getRoundId(), ex.getOptionId());
        
        ErrorResponse errorResponse = ErrorResponse.of("INVALID_OPTION_FOR_ROUND", ex.getMessage());
        return ResponseEntity.status(422).body(errorResponse);
    }

    @ExceptionHandler(GameException.class)
    public ResponseEntity<ErrorResponse> handleGameException(GameException e) {
        ErrorResponse errorResponse = ErrorResponse.of(
                e.getErrorCode().getCode(),
                e.getMessage(),
                e.getDetails()
        );
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        // Map common ResponseStatusException messages to error codes
        ErrorCode errorCode = mapToErrorCode(e);
        ErrorResponse errorResponse = ErrorResponse.of(errorCode.getCode(), e.getReason());
        
        return ResponseEntity
                .status(e.getStatusCode())
                .body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Bad Request: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_REQUEST.getCode(),
                e.getMessage()
        );
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(errorResponse);
    }

    /**
     * NotFoundException -> 404 Not Found (for /results API)
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException e, HttpServletRequest req) {
        // Special logging for results API
        if (req.getRequestURI().contains("/results/")) {
            String sessionId = req.getRequestURI().replaceAll(".*results/(\\d+).*", "$1");
            log.warn("[RESULT-ERR] sid={}, causeClass=NotFoundException, msg={}", sessionId, e.getMessage());
        } else {
            log.warn("Resource Not Found: path={}, msg={}", req.getRequestURI(), e.getMessage());
        }
        
        ErrorResponse errorResponse = ErrorResponse.of(
                "NOT_FOUND",
                e.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }
    
    /**
     * UnprocessableEntityException -> 422 Unprocessable Entity (for /results API)
     */
    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<ErrorResponse> handleUnprocessableEntity(UnprocessableEntityException e, HttpServletRequest req) {
        // Special logging for results API
        if (req.getRequestURI().contains("/results/")) {
            String sessionId = req.getRequestURI().replaceAll(".*results/(\\d+).*", "$1");
            log.error("[RESULT-ERR] sid={}, causeClass=UnprocessableEntity, msg={}", sessionId, e.getMessage());
        } else {
            log.warn("Unprocessable Entity: path={}, msg={}", req.getRequestURI(), e.getMessage());
        }
        
        ErrorResponse errorResponse = ErrorResponse.of(
                "UNPROCESSABLE_ENTITY",
                e.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorResponse);
    }

    /**
     * NoSuchElementException -> 404 Not Found
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException e) {
        log.warn("Resource Not Found: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.SESSION_NOT_FOUND.getCode(),
                e.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    /**
     * IllegalStateException -> 409 Conflict
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e, HttpServletRequest req) {
        log.warn("[ANSWERS-STATE] path={}, msg={}", req.getRequestURI(), e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                "INVALID_SESSION_OR_ROUND_STATE",
                e.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse);
    }

    /**
     * NullPointerException -> 400 Bad Request
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(NullPointerException e) {
        log.warn("Null Pointer Exception: {}", e.getMessage(), e);
        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_REQUEST.getCode(),
                "필수 파라미터가 누락되었습니다."
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * NumberFormatException -> 400 Bad Request
     */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ErrorResponse> handleNumberFormatException(NumberFormatException e) {
        log.warn("Number Format Exception: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_REQUEST.getCode(),
                "잘못된 숫자 형식입니다."
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * RuntimeException -> 400 Bad Request (500 방지를 위한 방어)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        log.error("Unexpected Runtime Exception: {}", e.getMessage(), e);
        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.INVALID_REQUEST.getCode(),
                "잘못된 요청입니다."
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e, HttpServletRequest req) {
        // Special logging for results API
        if (req.getRequestURI().contains("/results/")) {
            String sessionId = req.getRequestURI().replaceAll(".*results/(\\d+).*", "$1");
            log.error("[RESULT-ERR] path={}, sid={}, cause={}, msg={}", 
                req.getRequestURI(), sessionId, e.getClass().getSimpleName(), e.getMessage(), e);
        } else {
            log.error("[ANSWERS-UNKNOWN] path=" + req.getRequestURI(), e);
        }
        
        ErrorResponse errorResponse = ErrorResponse.of(
                "INTERNAL_ERROR",
                "unexpected error occurred"
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    private ErrorCode mapToErrorCode(ResponseStatusException e) {
        String reason = e.getReason();
        if (reason != null) {
            if (reason.contains("Session not found") || reason.contains("세션을 찾을 수 없습니다")) {
                return ErrorCode.SESSION_NOT_FOUND;
            }
            if (reason.contains("Member not found") || reason.contains("대상이 없습니다")) {
                return ErrorCode.MEMBER_NOT_FOUND;
            }
            if (reason.contains("Invalid code format") || reason.contains("코드 형식")) {
                return ErrorCode.INVALID_CODE_FORMAT;
            }
            if (reason.contains("Private room requires PIN") || reason.contains("PIN")) {
                return ErrorCode.PRIVATE_ROOM_PIN_REQUIRED;
            }
        }
        
        // Default mapping based on status code
        return switch (e.getStatusCode().value()) {
            case 404 -> ErrorCode.SESSION_NOT_FOUND;
            case 403 -> ErrorCode.NOT_HOST;
            case 409 -> ErrorCode.SESSION_FULL;
            default -> ErrorCode.INTERNAL_SERVER_ERROR;
        };
    }
}